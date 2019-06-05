(ns queues.models.events
  (:require [clojure.spec.alpha :as s]
            [queues.models.job :as job]
            [queues.models.agent :as agent]))

(s/def ::new-agent ::agent/agent)
(s/def ::new-job ::job/job)
(s/def ::job-request ::agent/id)

(s/def ::events (s/coll-of (s/or :new-agent   (s/keys :req [::new-agent])
                                 :new-job     (s/keys :req [::new-job])
                                 :job-request (s/keys :req [::job-request]))
                           :distinct true
                           :into []))