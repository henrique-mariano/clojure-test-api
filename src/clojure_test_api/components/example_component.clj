(ns clojure-test-api.components.example-component
  (:require [com.stuartsierra.component :as component]))

(defrecord ExampleComponent
  [config]
  ;; Implement the Lifecycle protocol
  component/Lifecycle

  (start [component]
         (println "Starting ExampleComponent")
         (assoc component :state ::started))

  (stop [component]
    (println "Stopping ExampleComponent")
    (assoc component :state nil)))

(defn new-example-component
  [config]
  (map->ExampleComponent {:config config}))