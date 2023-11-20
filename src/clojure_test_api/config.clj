(ns clojure-test-api.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

(defn read-config
  []
  (-> "resources/config.edn"
      io/resource
      aero/read-config))