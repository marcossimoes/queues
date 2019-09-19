(ns queues.io
  (:require [cheshire.core :as che]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [queues.json-converter :as json-converter]
            [queues.specs.queues :as specs.queues]
            [queues.specs.events :as specs.events]))

(defn- parse-file-encode
  [file]
  (re-find #"\.\w+\.\w+$" file))

(s/fdef parse-file-encode
        :args (s/cat :file string?)
        :ret string?)

(defn- is-json-file? [input-file]
  (->> input-file (parse-file-encode) (= ".json.txt")))

(s/fdef is-json-file?
        :args (s/cat :input-file (s/or :file string?
                                       :nil nil?))
        :ret boolean?)

(defn- str-from-file-content
  [input-file]
  (slurp input-file))

(s/fdef str-from-file-content
        :args (s/cat :input-file string?)
        :ret string?)

(defn str-from-json-file-content
  [input-json-file]
  (when (is-json-file? input-json-file)
    (str-from-file-content input-json-file)))

(s/fdef str-from-json-file-content
        :args (s/cat :input-json-file string?)
        :ret string?)

(defn output-job-queue-str-formatted-to-file!
  [jobs-assigned-str output-file]
  (->> jobs-assigned-str
       (spit output-file)))

(s/fdef output-job-queue-str-formatted-to-file!
        :args (s/cat :jobs-assigned any?
                     :output-file string?))