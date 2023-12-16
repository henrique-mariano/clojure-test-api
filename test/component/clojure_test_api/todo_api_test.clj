(ns component.clojure-test-api.todo-api-test
  (:require [clj-http.client :as http]
            [clojure-test-api.components.pedestal-component :refer [url-for]]
            [clojure-test-api.core :as core]
            [clojure.test :refer [deftest
                                  is
                                  testing]]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [util.clojure-test-api.util-test :refer [create-database-container
                                                     get-free-port
                                                     jdbc-url
                                                     sut->url
                                                     with-system]]))

(defn- system-map
  [actual-jdbc-url user password]
  {:server      {:port (get-free-port)}
   :datasource? true
   :db-spec     {:jdbcUrl (jdbc-url actual-jdbc-url
                                    user
                                    password)}})

(deftest get-list-test
  (let [database-container (create-database-container)]
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (try
      (.start database-container)
      (with-system
        [sut (core/clojure-test-api-system (system-map (.getJdbcUrl database-container)
                                                       (.getUsername database-container)
                                                       (.getPassword database-container)))]
        
        (let [{:keys [datasource]} sut
              {:todo/keys [id title]} (jdbc/execute-one!
                                       (datasource)
                                       (-> {:insert-into [:todo]
                                            :columns [:title]
                                            :values [["My todo list for test"]]
                                            :returning :*}
                                           (sql/format))
                                       {:builder-fn rs/as-kebab-maps})
              
              {:keys [status body]} (-> (sut->url sut
                                                  (url-for :db-get-list
                                                           {:path-params {:list-id id}}))
                                        (http/get {:accept :json
                                                   :as :json
                                                   :throw-exceptions false})
                                        (select-keys [:body :status]))]
          
          (testing "Create and get a list from db"
            (is (= status 200))
            (is (some? (:todo/created-at body)))
            (is (= #:todo{:id (str id) :title title}
                   (select-keys body [:todo/id :todo/title])))))

        (testing "Empty body is return for random list-id"
          (is (= {:body   ""
                  :status 404}
                 (-> (sut->url sut
                               (url-for :db-get-list
                                        {:path-params {:list-id (str (random-uuid))}}))
                     (http/get {:accept :json
                                :as :json
                                :throw-exceptions false})
                     (select-keys [:body :status]))))))
      (finally
        (.stop database-container)))))
