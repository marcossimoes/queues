(ns queues.json
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [queues.models.events :as events]
            [queues.models.agent :as agent]
            [queues.models.job :as job]
            [queues.models.job-assigned :as ja]))

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
    ::agent/id "agent_id"
    org-kw))

(defn js-kw->cj-kw
  "Receives a json formatted keyword and a namespace
  and returns an equivalent clojure keyword"
  [namespace js-kw]
  (->> js-kw
       (#(str/replace % #"_" "-"))
       (keyword namespace)))

(defn namespaced-kw-type
  [event]
  (->> event
       ((comp first keys))
       (js-kw->cj-kw "queues.models.events")))

(defn namespaced-kws-content
  [namespace event]
  (->> event
       ((comp first vals))
       (reduce-kv (fn [content k v]
                    (assoc content (js-kw->cj-kw (str "queues.models." namespace) k) v))
                  {})))

(defn namespaced-kws-event
  "Receives a jason formatted event and a namespace and returns
  that event's content with keywords transformed in namespaced
  symbols"
  [namespace event]
  (let [type (namespaced-kw-type event)
        content (namespaced-kws-content namespace event)]
    ;;(hash-map type content)
    content
    ))

(defmulti read-json-event (fn [_ event] ((comp first keys) event)))

(defmethod read-json-event "new_agent" [clj-events event]
  (->> event
       (namespaced-kws-event "agent")
       (conj clj-events)))

(defmethod read-json-event "new_job" [clj-events event]
  (->> event
       (namespaced-kws-event "job")
       (conj clj-events)))

(defmethod read-json-event "job_request" [clj-events event]
  (->> event
       (namespaced-kws-event "job-request")
       (conj clj-events)))

(defn read-json-events
  "Receives a string with json formatted events
  and returns a vector with clojure formatted events"
  [str]
  (->> str
       (json/read-str)
       (reduce read-json-event [])))

(defn write-json-events
  "Receives clj formatted events and returns json formatted events"
  [clj-events]
  (json/write-str :key-fn converted-kws clj-events))