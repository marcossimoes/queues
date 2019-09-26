(ns queues.init
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.db :as specs.db]
            [queues.specs.queues :as specs.queues]
            [queues.specs.matching-criteria :as specs.matching-criteria]
            [queues.specs.agents :as specs.agents]
            [queues.specs.order-of-priority :as specs.order-of-priority]))

(def ^:dynamic *logging* false)

(def default-opts {:input-file   "resources/sample-input.json.txt"
                   :log          false
                   :pretty-print false
                   :output-file  "jobs-assigned.json.txt"
                   :web-server   false})

;; TODO [QUESTION; ARCH] where should config parameters and default options be stored?

(defn db
  ([] (db nil))
  ([{init-agents              ::specs.agents/all-agents,
     init-jobs-waiting        ::specs.queues/jobs-waiting,
     init-jobs-in-progress    ::specs.queues/jobs-in-progress,
     init-jobs-done           ::specs.queues/jobs-done,
     init-job-requests-queued ::specs.queues/job-reqs-queued,
     init-order-of-priority   ::specs.order-of-priority/order-of-priority :as db-data}]
   {::specs.db/agents            (agent (if init-agents
                                          init-agents
                                          {})),
    ::specs.db/jobs-waiting      (ref (if init-jobs-waiting
                                        init-jobs-waiting
                                        [])),
    ::specs.db/jobs-in-progress  (ref (if init-jobs-in-progress
                                        init-jobs-in-progress
                                        [])),
    ::specs.db/jobs-done         (ref (if init-jobs-done
                                        init-jobs-done
                                        [])),
    ::specs.db/job-reqs-queued   (ref (if init-job-requests-queued
                                        init-job-requests-queued
                                        [])),
    ::specs.db/order-of-priority (if init-order-of-priority
                                   init-order-of-priority
                                   (let [skillset-key ::specs.matching-criteria/skillset
                                         primary-skillset ::specs.agents/primary-skillset
                                         secondary-skillset ::specs.agents/secondary-skillset
                                         urgent-key ::specs.matching-criteria/urgent]
                                     [{skillset-key primary-skillset, urgent-key true}
                                      {skillset-key primary-skillset, urgent-key false}
                                      {skillset-key secondary-skillset, urgent-key true}
                                      {skillset-key secondary-skillset, urgent-key false}]))}))

(s/fdef db
        :args (s/alt :nullary (s/cat)
                     :unary (s/or :db-data map?
                                  :nil nil?))
        :ret ::specs.db/db)

(def ^:dynamic *service-db* (db))