(ns component.clojure-test-api.info-test
  (:require [clj-http.client :as http]
            [clojure-test-api.core :as core]
            [clojure-test-api.components.pedestal-component :refer [url-for]]
            [clojure.test :refer [deftest is]]
            [util.clojure-test-api.util-test :refer [create-database-container
                                                     get-free-port
                                                     jdbc-url
                                                     sut->url
                                                     with-system]]))

#_{:clj-kondo/ignore [:unresolved-symbol]}
(deftest info-test
  (let [database-container (create-database-container)]
    (try
      (.start database-container)
      
      (with-system
        [sut (core/clojure-test-api-system {:server  {:port (get-free-port)}
                                            :db-spec {:jdbcUrl (jdbc-url (.getJdbcUrl database-container)
                                                                         (.getUsername database-container)
                                                                         (.getPassword database-container))}
                                            :datasource? true})]
        (is (= {:body   "Database server version: 15.4 (Debian 15.4-2.pgdg120+1)"
                :status 200}
               (-> (sut->url sut (url-for :info))
                   (http/get {:accept :json})
                   (select-keys [:body :status])))))
      
      (finally
        (.stop database-container)))))