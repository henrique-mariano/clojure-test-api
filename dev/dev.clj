(ns dev
  (:require [com.stuartsierra.component.repl :as component-repl]
            [clojure-test-api.core :as core]))

;; :jdbcUrl "jdbc:postgresql://localhost:5432/rwca?user=rwca&password=rwca"

(component-repl/set-init
 (fn [_old-system]
   (core/clojure-test-api-system
    {:server {:port 3001}
     :db-spec {:jdbcUrl "jdbc:postgresql://localhost:5432/rwca?user=rwca&password=rwca"}
     :datasource? true})))