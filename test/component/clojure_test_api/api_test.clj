(ns component.clojure-test-api.api-test
  (:require [clj-http.client :as http]
            [clojure-test-api.core :as core]
            [clojure-test-api.components.pedestal-component :refer [url-for]]
            [clojure.test :refer [deftest is testing]]
            [cheshire.core :as json]
            [util.clojure-test-api.util-test :refer [get-free-port
                                                     sut->url
                                                     with-system]]))

(def system-map
  {:server      {:port (get-free-port)}
   :datasource? false})

#_{:clj-kondo/ignore [:unresolved-symbol]}
(deftest greeting-test
  (with-system
    [sut (core/clojure-test-api-system system-map)]
    (prn "SUT!!")
    (prn sut)
    (is (= {:body   "Hello World!"
            :status 200}
           (-> (sut->url sut (url-for :greet))
               (http/get {:accept :json})
               (select-keys [:body :status]))))))

#_{:clj-kondo/ignore [:unresolved-symbol]}
(deftest content-negotiation-test
  (testing "only application/json is accepted"
    (with-system
      [sut (core/clojure-test-api-system system-map)]
      (is (= {:body   "Not Acceptable"
              :status 406}
             (-> (sut->url sut (url-for :greet))
                 (http/get {:accept :edn
                            :throw-exceptions false})
                 (select-keys [:body :status])))))))

(deftest get-list-test
  (let [list-id-1 (str (random-uuid))
        list-1    {:id list-id-1
                   :name "My todo list for test"
                   :items [{:id (str (random-uuid))
                            :name "finish"
                            :status "created"}]}]
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (with-system
      [sut (core/clojure-test-api-system system-map)]
      (reset! (-> sut :in-memory-state-component :state-atom)
              [list-1])
      (is (= {:body   list-1
              :status 200}
             (-> (sut->url sut
                           (url-for :get-list
                                    {:path-params {:list-id list-id-1}}))
                 (http/get {:accept :json
                            :as :json
                            :throw-exceptions false})
                 (select-keys [:body :status]))))

      (testing "Empty body is return for random list-id"
        (is (= {:body   ""
                :status 404}
               (-> (sut->url sut
                             (url-for :get-list
                                      {:path-params {:list-id (str (random-uuid))}}))
                   (http/get {:accept :json
                              :as :json
                              :throw-exceptions false})
                   (select-keys [:body :status]))))))))

(deftest post-list-test
  (let [list-id-1 (str (random-uuid))
        list-1    {:id list-id-1
                   :name "My todo list for test"
                   :items [{:id (str (random-uuid))
                            :name "finish"
                            :status "created"}]}
        invalid-list {:id list-id-1
                      :name "My todo list for test"
                      :items [{:name "finish"
                               :status "created"}]}]
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (with-system
      [sut (core/clojure-test-api-system system-map)]
      (testing "store and retrieve list by id"
        (is (= {:body   (json/encode list-1)
                :status 201}
               (-> (sut->url sut
                             (url-for :post-list))
                   (http/post {:content-type     :json
                               :throw-exceptions false
                               :body             (json/encode list-1)})
                   (select-keys [:body :status]))))

        (is (= {:body   list-1
                :status 200}
               (-> (sut->url sut
                             (url-for :get-list
                                      {:path-params {:list-id list-id-1}}))
                   (http/get {:accept :json
                              :as :json
                              :throw-exceptions false})
                   (select-keys [:body :status])))))

      (testing "invalid List is rejected"
        (is (= {:status 500}
               (-> (sut->url sut
                             (url-for :post-list))
                   (http/post {:content-type     :json
                               :throw-exceptions false
                               :body             (json/encode invalid-list)})
                   (select-keys [:status]))))))))

(deftest a-simple-api-test
  (is (= 1 1)))