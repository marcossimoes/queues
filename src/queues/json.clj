(ns queues.json
  (:require [clojure.string :as str]
            [cheshire.core :refer :all]
            [queues.models.specs :as specs]))

(defn converted-kws
  "Receives a original string keyword in json format
  and returns a keyword that is compatible with this apps models"
  [org-kw]
  (case org-kw
    ;;::events/new-agent "new_agent"
    ;;::events/new-job "new_job"
    ;;::events/job-request "job_request"
    ::specs/job-assigned "job_assigned"
    ::specs/job-assigned.job-id "job_id"
    ::specs/job-assigned.agent-id "agent_id"
    (name org-kw)))

;;TODO: refactor, with exception from ::job/id -> "job_id" all other cases can be writen in one rule (-> (name) (hyfen->underscore))

(defn js-kw->cj-kw
  "Receives a json formatted keyword and a namespace
  and returns an equivalent clojure keyword"
  [type js-kw]
  (->> js-kw
       (#(str/replace % #"_" "-"))
       (str type ".")
       (keyword "queues.models.specs")))

(defn event-content
  "Receives and event formatted as {type {content}}
  and returns content"
  [event]
  ((comp first vals) event))

(defn kworded-content
  "Receives a content and returns it with its keys keyworded
  from json to clj"
  [js-content type]
  (reduce-kv
    (fn [kw-content json-key json-value]
      (assoc kw-content (js-kw->cj-kw type json-key) json-value))
    {}
    js-content))

(defn typed-kworded-content
  "Receives a event content type and a jason formatted event and returns
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
       (hash-map ::specs/new-agent)
       (conj clj-events)))

(defmethod read-json-event "new_job" [clj-events event]
  (->> event
       (typed-kworded-content "job")
       (hash-map ::specs/new-job)
       (conj clj-events)))

(defmethod read-json-event "job_request" [clj-events event]
  (->> event
       (typed-kworded-content "job-req")
       (hash-map ::specs/job-request)
       (conj clj-events)))

(defn read-json-events
  "Receives a string with json formatted events
  and returns a vector with clojure formatted events"
  [str]
  (->> str
       (parse-string)
       (reduce read-json-event [])))

(defn write-json-events
  "Receives clj formatted events and returns json formatted events"
  [clj-events]
  (generate-string clj-events {:key-fn converted-kws
                               :pretty true}))