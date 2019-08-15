(ns queues.io
  (:require [clojure.string :as str]
            [cheshire.core :refer :all]
            [queues.specs.events :as specs.events]
            [queues.init :as init]
            [queues.specs.job-assigned :as specs.job-assigned]
            [clojure.pprint :as pp]))

(defn converted-kws
  "Receives a original string keyword in queues.io format
  and returns a keyword that is compatible with this apps specs"
  [org-kw]
  (case org-kw
    ::specs.job-assigned/job-assigned "job_assigned"
    ::specs.job-assigned/job-id "job_id"
    ::specs.job-assigned/agent-id "agent_id"
    (name org-kw)))

;;TODO: refactor, with exception from ::job/id -> "job_id" all other cases can be writen in one rule (-> (name) (hyfen->underscore))

(defn js-key->cj-ns-kwd-key
  "Receives a queues.io formatted keyword and a namespace
  and returns an equivalent clojure keyword"
  [nmspc js-kw]
  (->> js-kw
       (#(str/replace % #"_" "-"))
       (keyword (str "queues.specs." nmspc))))

(defn js-keys->cj-ns-kwd-keys
  "Receives a payload and returns it with its keys keyworded
  from queues.io to clj"
  [nmspc js-payload]
  (reduce-kv
    (fn [kw-payload json-key json-value]
      (let [clj-key (js-key->cj-ns-kwd-key nmspc json-key)
            clj-value json-value]
        (assoc kw-payload clj-key clj-value)))
    {}
    js-payload))

(defn ns-from-js-event-type
  [js-event-type]
  (str/replace js-event-type #"new_|_" {"_" "-" "new_" ""}))

(defn read-json-event
  [clj-events js-event]
  (let [[js-event-type js-event-payload] (first js-event)
        event-type (js-key->cj-ns-kwd-key "events" js-event-type)
        event-payload (-> js-event-type
                          (ns-from-js-event-type)
                          (js-keys->cj-ns-kwd-keys js-event-payload))]
    (conj clj-events (hash-map event-type event-payload))))

(defn read-json-events
  [json-str]
  (->> json-str
       (parse-string)
       (reduce read-json-event [])))

(defn read-json-file
  "Receives a string with queues.io formatted events
  and returns a vector with clojure formatted events"
  [input-file]
  (->> input-file
       (slurp)
       read-json-events))

(defn write-json-events
  "Receives clj formatted events and returns queues.io formatted events"
  [clj-events]
  (generate-string clj-events {:key-fn converted-kws :pretty true}))

(defn write-json-file
  [clj-events output-file]
  (->> clj-events
       write-json-events
       (spit output-file)))