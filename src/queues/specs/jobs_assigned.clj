(ns queues.specs.jobs-assigned
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job :as specs.job]
            [queues.specs.job-assigned :as specs.job-assigned]))


(s/def ::jobs-assigned (s/coll-of ::specs.job-assigned/job-assigned-map))
