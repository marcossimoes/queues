(ns queues.io-test
  (:require [cheshire.core :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [midje.sweet :refer :all]
            [queues.io :refer :all]
            [queues.test-cases :as cases]))

(let [str-from-file-content #'queues.io/str-from-file-content
      parse-file-encode #'queues.io/parse-file-encode
      is-json-file? #'queues.io/is-json-file?]

  (facts "str-from-file-content"
         (str-from-file-content "resources/sample-input-test.json.txt") => cases/json-events-str)
  (facts "output-job-queue-str-formatted-to-file!"
         (output-job-queue-str-formatted-to-file! cases/jobs-assigned-json-events-str "test-of-func:-output-job-queue-to-file")
         (slurp "test-of-func:-output-job-queue-to-file") => (slurp "resources/sample-output-test.json.txt"))
  (facts "parse-file-encode"
         (parse-file-encode "sample-input.json.txt") => ".json.txt")
  (facts "is-json-file?"
         (is-json-file? ".json.txt") => true
         ;; TODO [IMPROVE] transform test bellow in a property test where if given any string different from ".json.txt returns false
         (is-json-file? ".clj") => false))

(stest/instrument)