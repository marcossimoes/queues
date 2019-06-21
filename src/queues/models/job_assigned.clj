(ns queues.models.job-assigned
  (:require [clojure.spec.alpha :as s]
            [queues.models.job :as job]
            [queues.models.job-request :as jr]))

(s/def ::job-assigned (s/keys :req [::job/id ::jr/agent-id]))