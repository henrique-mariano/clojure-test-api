(ns util.clojure-test-api.util-test
  (:require [clojure-test-api.core :as core]
            [clojure.string :as s]
            [com.stuartsierra.component :as component]) 
  (:import (java.net ServerSocket)
           [org.testcontainers.containers PostgreSQLContainer]))

(defmacro with-system
  [[bound-var binding-expr] & body]
  `(let [~bound-var (component/start ~binding-expr)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))

(defn get-free-port
  []
  (with-open [socket (ServerSocket. 0)]
    (.getLocalPort socket)))

(defn sut->url
  [sut path]
  (s/join ["http://localhost:"
             (-> sut :pedestal-component :config :server :port)
             path]))

(defn jdbc-url
  [url user password]
  (s/join "&" [url
               (str "user=" user)
               (str "password=" password)]))

(defn create-database-container
  []
  (PostgreSQLContainer. "postgres:15.4"))

(defn datasource-only-system
  [config]
  (component/system-map
   :datasource
   (core/datasource-component config)))
