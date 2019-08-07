(ns queues.io
  (:require [clojure.string :as str]
            [cheshire.core :refer :all]
            [queues.specs.events :as specs.events]
            [queues.init :as init]
            [queues.specs.job-assigned :as specs.job-assigned]))

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

(defn js-kw->cj-kw
  "Receives a queues.io formatted keyword and a namespace
  and returns an equivalent clojure keyword"
  [type js-kw]
  (->> js-kw
       (#(str/replace % #"_" "-"))
       (keyword (str "queues.specs." type))))

(defn event-content
  "Receives and event formatted as {type {content}}
  and returns content"
  [event]
  ((comp first vals) event))

(defn kworded-content
  "Receives a content and returns it with its keys keyworded
  from queues.io to clj"
  [js-content type]
  (reduce-kv
    (fn [kw-content json-key json-value]
      (assoc kw-content (js-kw->cj-kw type json-key) json-value))
    {}
    js-content))

(defn typed-kworded-content
  "Receives a event content type and a json formatted event and returns
  that event's content with keywords transformed in namespaced
  symbols"
  [type event]
  (-> event
      (event-content)
      (kworded-content type)))

(defmulti read-json-event (fn [_ event] ((comp first keys) event)))

(defmethod read-json-event "new_agent" [clj-events event]
  (->> event
       (typed-kworded-content "agent")
       (hash-map ::specs.events/new-agent)
       (conj clj-events)))

(defmethod read-json-event "new_job" [clj-events event]
  (->> event
       (typed-kworded-content "job")
       (hash-map ::specs.events/new-job)
       (conj clj-events)))

(defmethod read-json-event "job_request" [clj-events event]
  (->> event
       (typed-kworded-content "job-request")
       (hash-map ::specs.events/job-request)
       (conj clj-events)))

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