(ns queues.json
  (:require [clojure.string :as str]
            [cheshire.core :refer :all]
            [queues.models.events :as events]
            [queues.models.job :as job]
            [queues.models.job-assigned :as ja]
            [queues.models.job-request :as jr]))

(defn converted-kws
  "Receives a original string keyword in json format
  and returns a keyword that is compatible with this apps models"
  [org-kw]
  (case org-kw
    "new_agent" ::events/new-agent
    "new_job" ::events/new-job
    "job_request" ::events/job-request
    ::ja/job-assigned "job_assigned"
    ::job/id "job_id"
    ::jr/agent-id "agent_id"
    org-kw))

(defn js-kw->cj-kw
  "Receives a json formatted keyword and a namespace
  and returns an equivalent clojure keyword"
  [namespace js-kw]
  (->> js-kw
       (#(str/replace % #"_" "-"))
       (keyword (str "queues.models." namespace))))

(defn event-content
  "Receives and event formatted as {type {content}}
  and returns content"
  [event]
  ((comp first vals) event))

(defn typed-content
  "Receives a content map and a type and adds the type as a key value
  pair inside the content map"
  [content type]
  (assoc content
    ;;(js-kw->cj-kw type "event-type")
    ::events/type
    (js-kw->cj-kw "events" type)))

(defn kworded-content
  "Receives a content and returns it with its keys keyworded
  from json to clj"
  [content type]
  (reduce-kv
    (fn [content json-key json-value]
      (assoc content (js-kw->cj-kw type json-key) json-value))
    {}
    content))

(defn typed-kworded-content
  "Receives a jason formatted event and a namespace and returns
  that event's content with keywords transformed in namespaced
  symbols"
  [content-type event]
  (-> event
      (event-content)
      (kworded-content content-type)))

(defmulti read-json-event (fn [_ event] ((comp first keys) event)))

(defmethod read-json-event "new_agent" [clj-events event]
  (->> event
       (typed-kworded-content "agent")
       (hash-map ::events/new-agent)
       (conj clj-events)))

(defmethod read-json-event "new_job" [clj-events event]
  (->> event
       (typed-kworded-content "job")
       (hash-map ::events/new-job)
       (conj clj-events)))

(defmethod read-json-event "job_request" [clj-events event]
  (->> event
       (typed-kworded-content "job-request")
       (hash-map ::events/job-request)
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
  ;;(generate-string clj-events {:key-fn converted-kws :pretty true})
  (generate-string clj-events {:key-fn converted-kws :pretty true}))