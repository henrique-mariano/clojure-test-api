(ns persistence.clojure-test-api.migrations-test 
  (:require [clojure.test :refer [deftest is]] 
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [util.clojure-test-api.util-test :refer [create-database-container
                                                     datasource-only-system
                                                     jdbc-url
                                                     with-system]]))

(defn- system-map
  [actual-jdbc-url user password]
  {:db-spec {:jdbcUrl (jdbc-url actual-jdbc-url
                                user
                                password)}})

(deftest migrations-test
  (let [database-container (create-database-container)]
    (try
      (.start database-container)
      #_{:clj-kondo/ignore [:unresolved-symbol]}
      (with-system
        [sut (datasource-only-system (system-map (.getJdbcUrl database-container)
                                                 (.getUsername database-container)
                                                 (.getPassword database-container)))]
        (let [{:keys [datasource]} sut
              [schema-version :as schema-versions]
              (jdbc/execute!
               (datasource)
               ["select * from schema_version"]
               {:builder-fn rs/as-unqualified-lower-maps})]
          (is (= 1 (count schema-versions)))
          (is (= {:description "add todo tables"
                  :script "V1__add_todo_tables.sql"
                  :success true}
                 (select-keys schema-version [:description :script :success])))))
      (finally
        (.stop database-container)))))

(deftest todo-table-test
  (let [database-container (create-database-container)]
    (try
      (.start database-container)
      #_{:clj-kondo/ignore [:unresolved-symbol]}
      (with-system
        [sut (datasource-only-system (system-map (.getJdbcUrl database-container)
                                                 (.getUsername database-container)
                                                 (.getPassword database-container)))]
        (let [{:keys [datasource]} sut
              insert-results (jdbc/execute!
                              (datasource)
                              ["INSERT INTO todo(title)
                                VALUES('my todo list'), ('other todo list')
                                RETURNING *"]
                              {:builder-fn rs/as-unqualified-lower-maps})
              select-results (jdbc/execute!
                              (datasource)
                              ["SELECT * FROM todo"]
                              {:builder-fn rs/as-unqualified-lower-maps})]
          (is (= 2
                 (count insert-results)
                 (count select-results)))
          (is (= #{"my todo list"
                   "other todo list"}
                 (->> insert-results (map :title) (into #{}))
                 (->> select-results (map :title) (into #{}))))))
        (finally
          (.stop database-container)))))
