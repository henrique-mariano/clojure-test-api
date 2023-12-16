(ns clojure-test-api.components.pedestal-component
  (:require [cheshire.core :as json]
            [com.stuartsierra.component :as component]
            [honey.sql :as sql]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [schema.core :as s]))

(defn response
  ([status]
   (response status nil))
  ([status body]
   (merge
    {:status status
     :headers {"Content-Type" "application/json"}}
    (when body {:body (json/encode body)}))))

(def ok (partial response 200))
(def created (partial response 201))
(def not-found (partial response 404))

(defn respond-hello [_request]
  {:status 200
   :body "Hello World!"})

(defn get-list-by-id
  [{:keys [in-memory-state-component]} list-id]
  (->> @(:state-atom in-memory-state-component)
       (filter (fn [list]
                 (= list-id (:id list))))
       first))

(def echo
  {:name :echo
   :enter
   (fn [context]
     (let [request (:request context)
           response (ok request)]
       (assoc context :response response)))})

(def get-list
  {:name :get-list
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [request (:request context)
           list' (get-list-by-id dependencies
                                (-> request
                                    :path-params
                                    :list-id))
           response (if list'
                      (ok list')
                      (not-found))]
       (assoc context :response response)))})

(def db-get-list
  {:name :db-get-list
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [{:keys [datasource]} dependencies
           list-id (-> context
                       :request
                       :path-params
                       :list-id
                       (parse-uuid))
           todo-list (jdbc/execute-one!
                      (datasource)
                      (-> {:select :*
                           :from :todo
                           :where [:= :id list-id]}
                          (sql/format))
                      {:builder-fn rs/as-kebab-maps})
           response (if todo-list
                      (ok todo-list)
                      (not-found))]
       (assoc context :response response)))})

(def info
  {:name :info
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [{:keys [datasource]} dependencies
           ;; (data-source) will get a connection from pool 
           db-response (first (jdbc/execute!
                               (datasource)
                               ["SHOW SERVER_VERSION"]))]

       (assoc context :response {:status 200
                                 :body (str "Database server version: "
                                            (:server_version db-response))})))})

(defn save-list!
  [{:keys [in-memory-state-component]} list']
  (swap! (:state-atom in-memory-state-component) conj list'))

(s/defschema ListItem
  {:id     s/Str
   :name   s/Str
   :status s/Str})

(s/defschema List
  {:id    s/Str
   :name  s/Str
   :items [ListItem]})

(def post-list
  {:name :post-list
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [list' (s/validate List (get-in context [:request :json-params]))]
       (save-list! dependencies list')
       (assoc context :response (created list'))))})

(defn inject-dependencies
  [dependencies]
  (interceptor/interceptor
   {:name  ::inject-dependencies
    :enter (fn [context]
             (assoc context :dependencies dependencies))}))

(def content-negotiation-interceptor
  (content-negotiation/negotiate-content ["application/json"]))

(def routes
  (route/expand-routes
   #{["/greet"                   :get respond-hello :route-name :greet]
     ["/info"                    :get info]
     ["/todo"                    :post   [(body-params/body-params) post-list]]
     ["/todo/:list-id"           :get    get-list]
     ["/db/todo/:list-id"        :get    db-get-list]
     ["/todo/:list-id"           :post   echo :route-name :list-item-create]
     ["/todo/:list-id/:item-id"  :get    echo :route-name :list-item-view]
     ["/todo/:list-id/:item-id"  :put    echo :route-name :list-item-update]
     ["/todo/:list-id/:item-id"  :delete echo :route-name :list-item-delete]}))

(def url-for (route/url-for-routes routes))

(defrecord PedestalComponent
           [config
            example-component
            datasource
            in-memory-state-component]
  component/Lifecycle

  (start [component]
    (println "Starting PedestalComponent")
    (let [server (-> {::http/routes routes
                      ::http/type   :jetty
                      ::http/join?  false
                      ::http/port   (-> config :server :port)}
                     http/default-interceptors
                     (update ::http/interceptors concat
                             [(inject-dependencies component)
                              content-negotiation-interceptor])
                     http/create-server
                     http/start)]
      (assoc component :server server)))

  (stop [component]
    (println "Stopping PedestalComponent")
    (when-let [server (:server component)]
      (http/stop server))
    (assoc component :server nil)))

(defn new-pedestal-component
  [config]
  (map->PedestalComponent {:config config}))