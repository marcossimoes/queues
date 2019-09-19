(ns queues.fixtures
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]
            [queues.specs.agents :as specs.agents]
            [queues.specs.events :as specs.events]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.queues :as specs.queues]))

(def runs-for-each-prop-tests 100)

(def agent-ids (gen/sample (gen/not-empty (s/gen ::specs.agents/id)) 5))
(def job-ids (gen/sample (gen/not-empty (s/gen ::specs.job/id)) 5))
(def job-types (gen/sample (gen/not-empty (s/gen ::specs.job/type)) 3))
(s/def ::primary-skillset (s/coll-of (set job-types) :distinct true :min-count 1 :max-count 2))
(s/def ::secondary-skillset (s/coll-of (set job-types) :distinct true :min-count 0 :max-count 2))
(def primary-skillsets (gen/sample (s/gen ::primary-skillset)))
(def secondary-skillsets (gen/sample (s/gen ::secondary-skillset)))

(def overrides-events
  {[:new-agent ::specs.agents/id]                 #(gen/elements agent-ids)
   [:new-agent ::specs.agents/primary-skillset]   #(gen/elements primary-skillsets)
   [:new-agent ::specs.agents/secondary-skillset] #(gen/elements secondary-skillsets)
   [:new-job ::specs.job/id]                     #(gen/elements job-ids)
   [:new-job ::specs.job/type]                   #(gen/elements job-types)
   [:job-request ::specs.job-request/agent-id]   #(gen/elements agent-ids)})

(def overrides-job-types
  {[::specs.job/type] #(gen/elements job-types)})

(defn gen-matching-events []
  (s/gen (s/coll-of ::specs.events/event
                    ;;:distinct true
                    :into []
                    :max-count 100)
         overrides-events))

(defn gen-type-matching-jobs []
  (s/gen ::specs.queues/job-queue overrides-job-types))

(defn- queue-size-reduced-by?
  [queue-before queue-after x]
  (= (- (count queue-before) x)
     (count queue-after)))

(defn removed-item-from-queue-if-not-empty?
  [original-queue altered-queue item-count]
  (if (empty? original-queue)
    (empty? altered-queue)
    (queue-size-reduced-by? original-queue altered-queue item-count)))

;; THIS COMMENTED OUT FUNCTIONS BELLOW WERE A FAILED TENTATIVE SOLUTION FOR GENERATING
;; A JOB QUEUE WITH JOBS WITH UNIQUE JOB-ID
;; THE SOLUTION WOULD KEEP GENERATING MULTIPLE JOBS WITH EMPTY STRING JOB ID

;;(defn job-has-id?
;;  [job id]
;;  (let [job-id (::specs.job/id job)]
;;    (= job-id id)))

;;(defn job-queue-has-job-with-id?
;;  [job-queue id]
;;  (some (partial job-has-id? id) job-queue))

;;(defn added-to-queue-if-id-is-unique
;;  [new-job-queue job-candidate]
;;  (let [job-candidate-id (::specs.job/id job-candidate)]
;;    (if (job-queue-has-job-with-id? new-job-queue job-candidate-id)
;;      new-job-queue
;;      (conj new-job-queue job-candidate))))

;;(defn eliminated-job-id-duplicity
;;  [job-queue]
;;  (let [job-queue-with-unique-job-ids []]
;;    (reduce added-to-queue-if-id-is-unique
;;            job-queue-with-unique-job-ids
;;            job-queue)))

;;(s/def ::job-queue-with-unique-job-ids
;;  (s/with-gen ::specs.queues.specs.queues/job-queue
;;              #(gen/fmap eliminated-job-id-duplicity
;;                         (s/gen ::specs.queues.specs.queues/job-queue))))

(s/def ::unique-ids (s/coll-of ::specs.job/id :distinct true))

(defn job-with-overrided-id [job new-id]
  (assoc job ::specs.job/id new-id))

(defn job-queue-with-job-ids-overrided [[queue new-job-ids]]
  (mapv job-with-overrided-id
        queue new-job-ids))

(def job-queue-with-unique-job-ids
  (gen/fmap job-queue-with-job-ids-overrided
            (gen/tuple (s/gen ::specs.queues/job-queue)
                       (s/gen ::unique-ids))))

;; TODO [IMPROVE, READ] seems to complex to improve readability. Alternatives bellow
(defn decreased-by-if-original-queue-not-empty?
  [size-new-queue size-original-queue x]
  (letfn [(size-decreased-by? []
            (= size-new-queue (- size-original-queue x)))
          (size-remained-zero? []
            (= 0 size-new-queue size-original-queue))]
    (if (> size-original-queue 0)
      (size-decreased-by?)
      (size-remained-zero?))))

;;(defn decreased-by-one-if-original-queue-not-empty?
;;  [size-new-queue size-original-queue]
;;  (= size-new-queue
;;     (if (> size-original-queue 0)
;;       size-original-queue
;;       (dec size-original-queue))))
;;
;;(defn decreased-by-one-if-original-queue-not-empty?
;;  [size-new-queue size-original-queue]
;;  (if (> size-original-queue 0)
;;    (= size-new-queue (dec size-original-queue))
;;    (= 0 size-new-queue size-original-queue)))

(defn event-type
  [event]
  (-> event keys first))

(defn is-job-request?
  [event]
  (= (event-type event) ::specs.events/job-request))

(defn is-job?
  [event]
  (= (event-type event) ::specs.events/new-job))

(defn inc-counter-if [condition counter event]
  (if (condition event) (inc counter) counter))

(defn num-events-that [total-events condition]
  (reduce (partial inc-counter-if condition) 0 total-events))

(stest/instrument)

;; TODO [QUESTION, ARCH] where should fixtures be located? should its functions be fdefed and tested? where should its tests be located?