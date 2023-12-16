(ns persistence.clojure-test-api.honeysql-test 
  (:require [clojure.test :refer [deftest is]]
            [honey.sql :as sql]
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
              select-query (sql/format {:select :*
                                        :from :schema-version})
              [schema-version :as schema-versions]
              (jdbc/execute!
               (datasource)
               select-query
               {:builder-fn rs/as-unqualified-lower-maps})]
          (is (= ["SELECT * FROM schema_version"]
                 select-query))
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
              insert-query (-> {:insert-into [:todo]
                                :columns [:title]
                                :values [["my todo list"]
                                         ["other todo list"]]
                                :returning :*}
                               (sql/format))
              select-query (sql/format {:select :*
                                        :from :todo})
              insert-results (jdbc/execute!
                              (datasource)
                              insert-query
                              {:builder-fn rs/as-unqualified-lower-maps})
              select-results (jdbc/execute!
                              (datasource)
                              select-query
                              {:builder-fn rs/as-unqualified-lower-maps})]
          (is (= ["INSERT INTO todo (title) VALUES (?), (?) RETURNING *"
                  "my todo list" "other todo list"]
                 insert-query))
          (is (= ["SELECT * FROM todo"]
                 select-query))
          (is (= 2
                 (count insert-results)
                 (count select-results)))
          (is (= #{"my todo list"
                   "other todo list"}
                 (->> insert-results (map :title) (into #{}))
                 (->> select-results (map :title) (into #{}))))))
        (finally
          (.stop database-container)))))
