(ns queues.json
  (:require [clojure.data.json :as json]
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

(defn read-json-events
  "Receives json formatted events and returns clojure formatted events"
  [json-events]
  (json/read-str :key-fn converted-kws json-events))

(defn write-json-events
  "Receives clj formatted events and returns json formatted events"
  [clj-events]
  (json/write-str :key-fn converted-kws clj-events))