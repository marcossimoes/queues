(ns queues.state-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [midje.sweet :refer :all]
            [queues.fixtures :as fix]
            [queues.init :as init]
            [queues.specs.agents :as specs.agents]
            [queues.specs.db :as specs.db]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.queues :as specs.queues]
            [queues.state :refer :all]
            [queues.test-cases :as cases]))

(facts "all-agents-indexed-by-id")
(facts "all-jobs-waiting")
(facts "all-jobs-in-progress")
(facts "all-jobs-done")
(facts "all-job-reqs-queued")
(facts "order-of-priority")
(facts "queue-agent")
(facts "queue-job-in-jobs-waiting")
(facts "queue-job-in-jobs-in-progress"
       (defspec added-job-to-jobs-in-progress
                fix/runs-for-each-prop-tests
                (prop/for-all [original-jobs-in-progress fix/job-queue-with-unique-job-ids
                               job-to-be-queued (s/gen ::specs.job/job)]
                              (let [db-init-vals {::specs.queues/jobs-in-progress original-jobs-in-progress}
                                    db (init/db db-init-vals)]
                                (dosync
                                  (queue-job-in-jobs-in-progress db job-to-be-queued)
                                  (-> (::specs.db/jobs-in-progress db)
                                      deref
                                      (.contains job-to-be-queued))))))
       (defspec increases-size-by-one
                fix/runs-for-each-prop-tests
                (prop/for-all [original-jobs-in-progress fix/job-queue-with-unique-job-ids
                               job-to-be-queued (s/gen ::specs.job/job)]
                              (let [db-init-vals {::specs.queues/jobs-in-progress original-jobs-in-progress}
                                    db (init/db db-init-vals)
                                    size-original-jobs-in-progress (count original-jobs-in-progress)]
                                (dosync
                                  (queue-job-in-jobs-in-progress db job-to-be-queued)
                                  (-> (::specs.db/jobs-in-progress db)
                                      deref
                                      count
                                      (= (inc size-original-jobs-in-progress)))))))
       )
(facts "queue-job-in-jobs-done")
(facts "queue-job-req")
(facts "update-agent-in-agents")
(facts "id-removed-from-job-queue"
       ;; TODO [Test] Increase test scenarios
       (fact "if provided a job-id and ::specs.job/id type removes tht job from queue"
             (id-removed-from-job-queue cases/job-queue cases/job-id-1t ::specs.job/id) => []))
(facts "remove-job-from-jobs-waiting"
       (fact "if db is empty, do nothing"
             (let [db (init/db)]
               (dosync
                 (remove-job-from-jobs-waiting db cases/job-1t)
                 (-> db ::specs.db/jobs-waiting deref) => empty?)))
       (fact "if db has job waiting remove it"
             (let [db-init-vals {::specs.queues/jobs-waiting [cases/job-1t]}
                   db (init/db db-init-vals)]
               (dosync
                 (remove-job-from-jobs-waiting db cases/job-1t)
                 (-> db ::specs.db/jobs-waiting deref) => (has not-any? #(= % cases/job-1t)))))
;;       ;; FIXME: this test is randomly failing sometimes. not sure why yet
       (fact "if db has job waiting, remove only one job"
             (let [jobs-waiting-sample (gen/generate (s/gen ::specs.queues/jobs-waiting))
                   jobs-waiting (conj jobs-waiting-sample cases/job-1t)
                   init-num-jobs-waiting (count jobs-waiting)
                   db-init-vals {::specs.queues/jobs-waiting jobs-waiting}
                   db (init/db db-init-vals)]
               (dosync
                 (remove-job-from-jobs-waiting db cases/job-1t)
                 (-> db ::specs.db/jobs-waiting deref count) => (dec init-num-jobs-waiting))))
       ;; TODO [IMPROVE, DRY] this is not DRY (appears multiple time in other tests. move to fixtures
       (defspec removes-matching-job-from-jobs-waiting-if-jobs-waiting-not-empty
                fix/runs-for-each-prop-tests
                (prop/for-all [original-jobs-waiting fix/job-queue-with-unique-job-ids]
                              (let [matching-job (if (not-empty original-jobs-waiting)
                                                   (first original-jobs-waiting)
                                                   cases/job-1t)
                                    matching-job-id (::specs.job/id matching-job)
                                    num-matching-jobs (fix/num-events-that original-jobs-waiting #(= (::specs.job/id %) matching-job-id))
                                    db-init-vals {::specs.queues/jobs-waiting original-jobs-waiting}
                                    db (init/db db-init-vals)]
                                (dosync
                                  (remove-job-from-jobs-waiting db matching-job)
                                  (->> (::specs.db/jobs-waiting db)
                                       deref
                                       ;; TODO [READ] improve readability
                                       (#(fix/removed-item-from-queue-if-not-empty? original-jobs-waiting % num-matching-jobs)))))))
       (defspec decreases-size-by-one-if-job-matches-anything-in-jobs-waiting
                fix/runs-for-each-prop-tests
                (prop/for-all [original-jobs-waiting fix/job-queue-with-unique-job-ids]
                              (let [matching-job (if (not-empty original-jobs-waiting)
                                                   (first original-jobs-waiting)
                                                   cases/job-1t)
                                    db-init-vals {::specs.queues/jobs-waiting original-jobs-waiting}
                                    db (init/db db-init-vals)
                                    size-original-jobs-waiting (count original-jobs-waiting)]
                                (dosync
                                  (remove-job-from-jobs-waiting db matching-job)
                                  (-> (::specs.db/jobs-waiting db)
                                      deref
                                      count
                                      (fix/decreased-by-if-original-queue-not-empty? size-original-jobs-waiting 1))))))
       (defspec does-nothing-when-job-does-not-match-anything-in-jobs-waiting
                fix/runs-for-each-prop-tests
                (prop/for-all [jobs-waiting-sample fix/job-queue-with-unique-job-ids]
                              (let [matching-job (if (not-empty jobs-waiting-sample)
                                                   (first jobs-waiting-sample)
                                                   cases/job-1t)
                                    original-jobs-waiting (rest jobs-waiting-sample)
                                    db-init-vals {::specs.queues/jobs-waiting original-jobs-waiting}
                                    db (init/db db-init-vals)]
                                (dosync
                                  (remove-job-from-jobs-waiting db matching-job)
                                  (->> (::specs.db/jobs-waiting db)
                                       deref
                                       (= original-jobs-waiting))))))
       )
(facts "remove-job-from-jobs-in-progress")
(facts "remove-job-req-from-job-reqs-queued"
       (fact "if db is empty, do nothing"
             (let [db (init/db)]
               (dosync
                 (remove-job-req-from-job-reqs-queued db cases/job-req-p1)
                 (-> db ::specs.db/job-reqs-queued deref) => empty?)))
       (fact "if db has job-req queued remove it"
             (let [db-init-vals {::specs.queues/job-reqs-queued [cases/job-req-p1]}
                   db (init/db db-init-vals)]
               (dosync
                 (remove-job-req-from-job-reqs-queued db cases/job-req-p1)
                 (-> db ::specs.db/job-reqs-queued deref) => (has not-any? #(= % cases/job-req-p1)))))
       ;; TODO [QUESTION, TEST] any reason to keep unit tests above when I am testing it on property testing?
       ;; FIXME test bellow 'if db has job-req queued, remove only one job-req' is randomly failing
       (fact "if db has job-req queued, remove the exact number of job-reqs that match the job-req-p1"
             (let [job-reqs-queued-sample (gen/generate (s/gen ::specs.queues/job-reqs-queued))
                   job-reqs-queued (conj job-reqs-queued-sample cases/job-req-p1)
                   init-num-job-reqs-queued (count job-reqs-queued)
                   job-reqs-with-job-req-p1-id (fix/num-events-that job-reqs-queued
                                                                    #(= (::specs.job-request/agent-id %)
                                                                        (::specs.job-request/agent-id cases/job-req-p1)))
                   db-init-vals {::specs.queues/job-reqs-queued job-reqs-queued}
                   db (init/db db-init-vals)]
               (dosync
                 (remove-job-req-from-job-reqs-queued db cases/job-req-p1)
                 (-> db ::specs.db/job-reqs-queued deref count) => (- init-num-job-reqs-queued job-reqs-with-job-req-p1-id))))
       ;;FIXME test not passing
       ;;(defspec removes-matching-job-req-from-queue-if-queue-not-empty
       ;;         fix/runs-for-each-prop-tests
       ;;         (prop/for-all [original-job-reqs-queued (s/gen ::specs.queues/job-reqs-queued)]
       ;;                       (let [matching-job-req (if (not-empty original-job-reqs-queued)
       ;;                                                (first original-job-reqs-queued)
       ;;                                                cases/job-req-p1)
       ;;                             num-matching-job-reqs (fix/num-events-that original-job-reqs-queued
       ;;                                                                        #(= % matching-job-req))
       ;;                             db-init-vals {::specs.queues/job-reqs-queued original-job-reqs-queued}
       ;;                             db (init/db db-init-vals)]
       ;;                         (dosync
       ;;                           (remove-job-req-from-job-reqs-queued db matching-job-req)
       ;;                           (->> (::specs.db/job-reqs-queued db)
       ;;                                deref
       ;;                                (#(fix/removed-item-from-queue-if-not-empty? original-job-reqs-queued % num-matching-job-reqs)))))))
       ;;(defspec decreases-size-by-the-exact-number-of-matching-job-reqs
       ;;         fix/runs-for-each-prop-tests
       ;;         (prop/for-all [original-job-reqs-queued (s/gen ::specs.queues/job-reqs-queued)]
       ;;                       (let [matching-job-req (if (not-empty original-job-reqs-queued)
       ;;                                                (first original-job-reqs-queued)
       ;;                                                cases/job-req-p1)
       ;;                             db-init-vals {::specs.queues/job-reqs-queued original-job-reqs-queued}
       ;;                             db (init/db db-init-vals)
       ;;                             size-original-job-reqs-queued (count original-job-reqs-queued)
       ;;                             job-reqs-with-matching-job-req-agent-id (fix/num-events-that original-job-reqs-queued
       ;;                                                                                          #(= % matching-job-req))]
       ;;                         (dosync
       ;;                           (remove-job-req-from-job-reqs-queued db matching-job-req)
       ;;                           (-> (::specs.db/job-reqs-queued db)
       ;;                               deref
       ;;                               count
       ;;                               (fix/decreased-by-if-original-queue-not-empty? size-original-job-reqs-queued
       ;;                                                                              job-reqs-with-matching-job-req-agent-id))))))
       ;;;; FIXME test bellow would need for fixtures to be able to generate unique job-reqs-queued
       ;;(defspec does-nothing-when-job-req-does-not-match-anything-in-job-reqs-queued
       ;;         fix/runs-for-each-prop-tests
       ;;         (prop/for-all [job-reqs-sample (s/gen ::specs.queues/job-reqs-queued)]
       ;;                       (let [unique-job-reqs-sample ((comp vec set) job-reqs-sample)
       ;;                             matching-job-req (if (not-empty unique-job-reqs-sample)
       ;;                                                (first unique-job-reqs-sample)
       ;;                                                cases/job-req-p1)
       ;;                             original-job-reqs-queued (rest unique-job-reqs-sample)
       ;;                             db-init-vals {::specs.queues/job-reqs-queued original-job-reqs-queued}
       ;;                             db (init/db db-init-vals)]
       ;;                         (dosync
       ;;                           (remove-job-req-from-job-reqs-queued db matching-job-req)
       ;;                           (->> (::specs.db/job-reqs-queued db)
       ;;                                deref
       ;;                                (= original-job-reqs-queued))))))
       )
(facts "agent-with-id")
(facts "job-in-progress-with-assigned-agent-id")

(stest/instrument)

;; TODO [TEST] finish tests state-test