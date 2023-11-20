(ns persistence.clojure-test-api.simple-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [next.jdbc :as jdbc])
  (:import (org.testcontainers.containers PostgreSQLContainer)))

(defn jdbc-url
  [url user password]
  (str/join "&" [url
                 (str "user=" user)
                 (str "password=" password)]))

(deftest a-simple-test-persistence-test
  (let [database-container (PostgreSQLContainer. "postgres:15.4")]
    (try
      (.start database-container)

      (let [ds (jdbc/get-datasource {:jdbcUrl (jdbc-url (.getJdbcUrl database-container)
                                                        (.getUsername database-container)
                                                        (.getPassword database-container))})]
        (is (= {:r 1} (first (jdbc/execute! ds ["select 1 as r;"])))))

      (finally
        (.stop database-container)))))