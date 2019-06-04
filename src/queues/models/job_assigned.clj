(ns queues.models.job-assigned
  (:require [clojure.spec.alpha :as s]
            [queues.models.job :as job]
            [queues.models.agent :as agent]))

(s/def ::job-assigned (s/keys :req [::job/id ::agent/id]))