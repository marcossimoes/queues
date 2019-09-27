(ns queues.controllers.new-event-processor
  (:require [clojure.pprint :as pp]
            [clojure.spec.alpha :as s]
            [queues.cli :as cli]
            [queues.init :as init]
            [queues.controllers.matcher :as matcher]
            [queues.logic.events :as logic.events]
            [queues.logic.jobs :as logic.jobs]
            [queues.specs.agents :as specs.agents]
            [queues.specs.db :as specs.db]
            [queues.specs.events :as specs.events]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-req]
            [queues.state :as state]))

(defn move-job-to-done!
  [db {agent-id ::specs.job-req/agent-id}]
  (when-let [job (state/job-in-progress-with-assigned-agent-id db agent-id)]
    (state/remove-job-from-jobs-in-progress db job)
    (state/queue-job-in-jobs-done db job)))

(s/fdef move-job-to-done!
        :args (s/cat :db ::specs.db/db
                     :job-req ::specs.job-req/job-request))

(defn assign-agent-to-job
  [db job {agent-id ::specs.job-req/agent-id :as job-req}]
  ;; TODO [QUESTION; ARCH] should I treat the case of possible empty job-req-queued in the controller or the state?
  (state/remove-job-req-from-job-reqs-queued db job-req)
  (state/remove-job-from-jobs-waiting db job)
  (->> job
       (logic.jobs/job-with-agent-assigned agent-id)
       (state/queue-job-in-jobs-in-progress db)))

(s/fdef assign-agent-to-job
        :args (s/cat :db ::specs.db/db
                     :job ::specs.job/job
                     :job-req ::specs.job-req/job-request))

(defmulti processed-event-by-type (fn [_ new-event] (logic.events/event-type new-event)))

(defmethod processed-event-by-type ::specs.events/new-agent
  [db {agent ::specs.events/new-agent}]
  ;; TODO [IMPROVE; FUNCTIONALITY] when new-agent is entered check for corresponding job-request and new-jobs
  ;; TODO [READ] make it clear that when an agent is entered for the second time the previous values are overriten
  (state/queue-agent db agent)
  ;; by returning the same agent inputed but from a direct call from db
  ;; we insure that the API response is actually tied to that agent being stored in db
  (state/agent-with-id db (::specs.agents/id agent)))

(defmethod processed-event-by-type ::specs.events/new-job
  [db {job ::specs.events/new-job :as new-job-event}]
  (dosync
    (if-let [job-req-matching-new-job (matcher/queued-object-matching db new-job-event)]
      (assign-agent-to-job db job job-req-matching-new-job)
      ;; TODO [IMPROVE; FUNCTIONALITY] treat for when a job is rentered for the second time
      (state/queue-job-in-jobs-waiting db job))))

(defmethod processed-event-by-type ::specs.events/job-request
  [db {job-req ::specs.events/job-request :as job-req-event}]
  (dosync
    (move-job-to-done! db job-req)
    (if-let [job-matching-job-req (matcher/queued-object-matching db job-req-event)]
      (assign-agent-to-job db job-matching-job-req job-req)
      ;; TODO [IMPROVE] What should happend if the system gets a second job-request from the same agent before the first job-request gets assigned?
      (state/queue-job-req db job-req))))

(s/fdef processed-event-by-type
        :args (s/cat :db ::specs.db/db
                     :event ::specs.events/event))

;; TODO [EXCEPTIONS] handle exception when event-type is null

(defn processed-event-with-log!
  [db event]
  (let [res (processed-event-by-type db event)]
    (if init/*logging*
      (cli/log-event-res-and-jqs-to-cli-and-return-res! event res db)
      res)))

(s/fdef processed-event-with-log!
        :args (s/cat :db ::specs.db/db
                     :event ::specs.events/event))

;; TODO [NXT] Make processed-event-by-type return the sata necessary for the API returns