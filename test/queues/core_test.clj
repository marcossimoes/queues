(ns queues.core-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [cheshire.core :refer :all]
            [midje.sweet :refer :all]
            [queues.core :refer :all]))

(facts "-main"
       ;;(fact "receives a sample-input file and returns a sample-output file"
       ;;      (-main "resources/sample-input.json.txt")
       ;;      (slurp "jobs-assigned.json.txt") => (slurp "resources/sample-output-test.json.txt"))
       )

(stest/instrument)

;; TODO [TEST] queues.core