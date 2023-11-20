(ns unit.clojure-test-api.simple-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure-test-api.components.pedestal-component :refer [url-for]]))

(deftest a-simple-passing-test
  (is (= 1 1)))

(deftest url-for-test
  (testing "Greet endpoint url"
    (is (= "/greet"
           (url-for :greet))))

  (testing "get list by id endpoint url"
    (let [list-id (random-uuid)]
      (is (= (str "/todo/" list-id)
             (url-for :get-list {:path-params {:list-id list-id}}))))))