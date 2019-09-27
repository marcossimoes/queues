(ns queues.controllers.matcher
  (:require [clojure.pprint :as pp]
            [clojure.spec.alpha :as s]
            [queues.logic.agents :as agents]
            [queues.logic.events :as logic.events]
            [queues.logic.matcher :as matcher]
            [queues.specs.agents :as specs.agents]
            [queues.specs.db :as specs.db]
            [queues.specs.events :as specs.events]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-req]
            [queues.specs.order-of-priority :as specs.order-of-priority]
            [queues.specs.queues :as specs.queues]
            [queues.state :as state]))

(defn- agent-with-job-req-queued
  [db {agent-id ::specs.job-req/agent-id :as job-req}]
  (->> agent-id
       ;; TODO [IMPROVE; ARCH] this seemed to buried in the controller and to recurrent to be a call in the db
       (state/agent-with-id db)))

(s/fdef agent-with-job-req-queued
        :args (s/cat :db ::specs.db/db
                     :job-req ::specs.job-req/job-request)
        :ret (s/or :agent-found ::specs.agents/agent
                   :agent-not-found nil?)
        :fn #(= (-> % :args :job-req ::specs.job-req/job-request)
                (-> % :ret ::specs.agents/agent)))

(defn- agents-from-job-reqs [db job-reqs-queued]
  (keep (partial agent-with-job-req-queued db) job-reqs-queued))

(s/fdef agents-from-job-reqs
        :args (s/cat :db ::specs.db/db
                     :job-reqs-queued ::specs.queues/job-reqs-queued)
        :ret ::specs.queues/agents-waiting-for-job
        :fn #(>= (-> % :args :job-reqs-queued count)
                 (-> % :ret count)))

(defmulti queued-object-matching (fn [_ new-event] (logic.events/event-type new-event)))

(defmethod queued-object-matching ::specs.events/new-job
  [db {{job-type ::specs.job/type} ::specs.events/new-job :as new-job-event}]
  (let [job-reqs-queued (state/all-job-reqs-queued db)
        agents-waiting (agents-from-job-reqs db job-reqs-queued)
        agent-matching-job (matcher/first-agent-matching-job-type job-type agents-waiting)]
    (when agent-matching-job
      (agents/job-req-from-agent agent-matching-job))))

(defmethod queued-object-matching ::specs.events/job-request
  [db {{job-req ::specs.job-req/agent-id} ::specs.events/job-request :as job-req-event}]
  (let [order-of-priority (state/order-of-priority db)
        jobs-waiting (state/all-jobs-waiting db)
        agent-requesting-job (state/agent-with-id db job-req)]
    ;; TODO [READ] make it more clear the 'when' bellow is checking for a present agent
    (when agent-requesting-job
      (matcher/first-job-matching-agent agent-requesting-job
                                        jobs-waiting
                                        order-of-priority))))

(s/fdef queued-object-matching
        :args (s/cat :db ::specs.db/db
                     :event ::specs.events/event)
        :ret (s/or :job ::specs.job/job
                   :job-req ::specs.job-req/job-request))