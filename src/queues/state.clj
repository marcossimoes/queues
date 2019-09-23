(ns queues.state
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.agents :as specs.agents]
            [queues.specs.db :as specs.db]
            [queues.specs.general :as specs.general]
            [queues.specs.job :as specs.job]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.specs.job-request :as specs.job-req]
            [queues.specs.order-of-priority :as specs.order-of-priority]
            [queues.specs.queues :as specs.queues]
            [clojure.pprint :as pp]))

;; TODO [QUESTION, READ] is it okay to compress functions like this when they are short and similar?

(defn all-agents-indexed-by-id [db]
  (-> db ::specs.db/agents deref))

(s/fdef all-agents-indexed-by-id
        :args (s/cat :db ::specs.db/db)
        :ret ::specs.agents/all-agents)

(defn all-jobs-waiting [db]
  (-> db ::specs.db/jobs-waiting deref))

(s/fdef all-jobs-waiting
        :args (s/cat :db ::specs.db/db)
        :ret ::specs.queues/jobs-waiting)

(defn all-jobs-in-progress [db]
  (-> db ::specs.db/jobs-in-progress deref))

(s/fdef all-jobs-in-progress
        :args (s/cat :db ::specs.db/db)
        :ret ::specs.queues/jobs-in-progress)

(defn all-jobs-done [db]
  (-> db ::specs.db/jobs-done deref))

(s/fdef all-jobs-done
        :args (s/cat :db ::specs.db/db)
        :ret ::specs.queues/jobs-done)

(defn all-job-reqs-queued [db]
  (-> db ::specs.db/job-reqs-queued deref))

(s/fdef all-job-reqs-queued
        :args (s/cat :db ::specs.db/db)
        :ret ::specs.queues/job-reqs-queued)

(defn order-of-priority [db]
  (-> db ::specs.db/order-of-priority))

(s/fdef order-of-priority
        :args (s/cat :db ::specs.db/db)
        :ret ::specs.order-of-priority/order-of-priority)

(defn queue-agent
  [db new-agent]
  ;; TODO [QUESTION; ARCH] is it okay to add this transformation here as it does not interfer with logic only with agent searching which is from state scope anyway?
  (let [agent-id (::specs.agents/id new-agent)]
    (-> db ::specs.db/agents (send assoc agent-id new-agent))))

(s/fdef queue-agent
        :args (s/cat :db ::specs.db/db
                     :new-agent ::specs.agents/agent))

(defn queue-job-in-jobs-waiting [db job]
  (-> db ::specs.db/jobs-waiting (commute conj job)))

(s/fdef queue-job-in-jobs-waiting
        :args (s/cat :db ::specs.db/db
                     :job ::specs.job/job)
        :ret ::specs.queues/jobs-waiting)

(defn queue-job-in-jobs-in-progress [db job]
  (-> db ::specs.db/jobs-in-progress (commute conj job)))

(s/fdef queue-job-in-jobs-in-progress
        :args (s/cat :db ::specs.db/db
                     :job ::specs.job/job)
        :ret ::specs.queues/jobs-in-progress
        :fn #(= 1 (- (-> % :ret count)
                     (-> % :args :db ::specs.db/jobs-in-progress deref count))))

(defn queue-job-in-jobs-done [db job]
  (-> db ::specs.db/jobs-done (commute conj job)))

(s/fdef queue-job-in-jobs-done
        :args (s/cat :db ::specs.db/db
                     :job ::specs.job/job)
        :ret ::specs.queues/jobs-done
        :fn #(= 1 (- (-> % :ret count)
                     (-> % :args :db ::specs.db/jobs-done deref count))))

(defn queue-job-req [db job-req]
  (-> db ::specs.db/job-reqs-queued (commute conj job-req)))

(s/fdef queue-job-req
        :args (s/cat :db ::specs.db/db
                     :job-req ::specs.job-req/job-request)
        :ret ::specs.db/job-reqs-queued)

;; TODO [QUESTION, READ] for agents is the queue terminology appropriate since both agents are not actually a queue of objects queued to be removed from that place?

(defn update-agent-in-agents
  [db new-agent]
  (let [agent-id (::specs.agents/id new-agent)
        agents (::specs.db/agents db)]
    (send agents #(assoc % agent-id new-agent))))

(s/fdef update-agent-in-agents
        :args (s/cat :db ::specs.db/db
                     :new-agent ::specs.agents/agent))

;; TODO [QUESTION, ARCH] should these nxt three functions be in controller or state?

(defn id-removed-from-job-queue
  ([job-queue id] (id-removed-from-job-queue job-queue id identity))
  ([job-queue id id-type]
   (reduce (fn [new-jq m]
             (if (= (id-type m) id)
               new-jq
               (conj new-jq m)))
           []
           job-queue)))

(s/fdef id-removed-from-job-queue
        :args (s/alt :duary (s/cat :job-queue coll?
                                   :id ::specs.general/id)
                     :triary (s/cat :job-queue coll?
                                    :id ::specs.general/id
                                    ;; TODO [IMPROVE] it could be an or func or keyword
                                    :id-type any?)))

(defn remove-job-from-jobs-waiting
  [db job]
  (let [jobs-waiting (all-jobs-waiting db)
        new-jobs-waiting (id-removed-from-job-queue jobs-waiting
                                                    (::specs.job/id job)
                                                    ::specs.job/id)]
    (ref-set (::specs.db/jobs-waiting db) new-jobs-waiting)))

(s/fdef remove-job-from-jobs-waiting
        :args (s/cat :db ::specs.db/db
                     :job ::specs.job/job)
        :ret ::specs.queues/jobs-waiting)

(defn remove-job-from-jobs-in-progress
  [db job]
  (let [jobs-in-progress (all-jobs-in-progress db)
        new-jobs-in-progress (id-removed-from-job-queue jobs-in-progress
                                                        (::specs.job/id job)
                                                        ::specs.job/id)]
    (ref-set (::specs.db/jobs-in-progress db) new-jobs-in-progress)))

(s/fdef remove-job-from-jobs-waiting
        :args (s/cat :db ::specs.db/db
                     :job ::specs.job/job)
        :ret ::specs.queues/jobs-waiting)

(defn remove-job-req-from-job-reqs-queued
  [db job-req]
  (let [job-reqs-queued (all-job-reqs-queued db)
        new-job-reqs-queued (id-removed-from-job-queue job-reqs-queued
                                                       (::specs.job-req/agent-id job-req)
                                                       ::specs.job-req/agent-id)]
    (ref-set (::specs.db/job-reqs-queued db) new-job-reqs-queued)))

(s/fdef remove-job-req-from-job-reqs-queued
        :args (s/cat :db ::specs.db/db
                     :job-req ::specs.job-req/job-request)
        :ret ::specs.queues/job-reqs-queued)

;; TODO [QUESTION, ARCH] should these last two functions be in state or controller?

;; TODO [IMPROVE, ARCH] `agent-with-id` embeds the logic of how agents is persisted

(defn agent-with-id
  [db agent-id]
  (let [agents (all-agents-indexed-by-id db)]
    (get agents agent-id)))

(s/fdef agent-with-id
        :args (s/cat :db ::specs.db/db
                     :agent-id ::specs.job-req/agent-id)
        :ret (s/or :found ::specs.agents/agent
                   :not-found nil?)
        :fn (s/or :found #(= (-> % :ret :found ::specs.agents/id)
                             (-> % :args :agent-id))
                  :not-found #(nil? (-> :ret :not-found %))))

(defn job-in-progress-with-assigned-agent-id
  [db agent-id]
  (->> db
       (all-jobs-in-progress)
       (filter #(= agent-id (::specs.job/assigned-agent %)))
       (first)))

(s/fdef job-in-progress-with-assigned-agent-id
        :args (s/cat :db ::specs.db/db
                     :agent-id ::specs.job-req/agent-id)
        :ret (s/or :job-found ::specs.job/job
                   :job-not-found nil?))