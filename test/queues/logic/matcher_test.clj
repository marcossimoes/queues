(ns queues.logic.matcher-test
  (:require [clojure.spec.test.alpha :as stest]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [midje.sweet :refer :all]
            [queues.fixtures :as fix]
            [queues.logic.matcher :refer :all]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job :as specs.job]
            [queues.specs.matching-criteria :as specs.matching-criteria]
            [queues.specs.order-of-priority :as specs.order-of-priority]))


;; To make this test more readable and concise it seemed right to create a abbreviation standard to make it
;; clear which skills and urgency each agent and job has at each time without being so verbose as to
;; expose all the agent and job structure. The convention is the following
;;
;; agent w/ skills 1 and 2 in primary skillset and skills 3 and 4 in secondary skill-set: agent-p12-s34
;; agents w/ agent-p12-s34 and agent p34-s56: agents-p12s34-p34s56
;; job w/ type matching skill 1 and urgency true: job-1t
;; jobs containing jobs 1t, 2t and 3f: jobs-1t2t3f


(let [first-job-matching-skill-and-urgency #'queues.logic.matcher/first-job-matching-skill-and-urgency
      first-job-matching-agent-on-specific-criteria #'queues.logic.matcher/first-job-matching-agent-on-specific-criteria
      agent-matches-job-type-through-skillset? #'queues.logic.matcher/agent-matches-job-type-through-skillset?
      first-agent-matching-job-type-through-skillset #'queues.logic.matcher/first-agent-matching-job-type-through-skillset

      skill-1 "bills"
      skill-2 "rewards"
      skill-3 "charge-back"
      skill-4 "payments"
      skill-5=skill-1 skill-1
      skill-6 "acquisition"

      empty-skillsets-agent #::specs.agents{:id                 "1",
                                           :name               "Gabriela Lima",
                                           :primary-skillset   [],
                                           :secondary-skillset []}
      agent-p1 #::specs.agents{:id                 "2",
                              :name               "Gabriela Lima",
                              :primary-skillset   [skill-1],
                              :secondary-skillset []}
      agent-p12  #::specs.agents{:id                 "3",
                                :name               "Gabriela Lima",
                                :primary-skillset   [skill-1 skill-2],
                                :secondary-skillset []}
      agent-p1-s6 #::specs.agents{:id                 "4",
                                 :name               "Gabriela Lima",
                                 :primary-skillset   [skill-1],
                                 :secondary-skillset [skill-6]}
      agent-p2 #::specs.agents{:id                 "5",
                              :name               "Gabriela Lima",
                              :primary-skillset   [skill-2],
                              :secondary-skillset []}
      agent-p2-s1 #::specs.agents{:id                 "6",
                                 :name               "Gabriela Lima",
                                 :primary-skillset   [skill-2],
                                 :secondary-skillset [skill-1]}
      agent-p5=1 #::specs.agents{:id                 "7",
                                :name               "Gabriela Lima",
                                :primary-skillset   [skill-5=skill-1],
                                :secondary-skillset []}
      agent-p6 #::specs.agents{:id                 "8",
                              :name               "Gabriela Lima",
                              :primary-skillset   [skill-6],
                              :secondary-skillset []}
      agent-p6-s4 #::specs.agents{:id                 "9",
                                 :name               "Gabriela Lima",
                                 :primary-skillset   [skill-6],
                                 :secondary-skillset [skill-4]}

      empty-agents-waiting []
      agents-waiting-p1 [agent-p1]
      agents-waiting-p1-p5=1 [agent-p1 agent-p5=1]
      agents-waiting-p6-p1-p5=1 [agent-p6 agent-p1 agent-p5=1]
      agents-waiting-p6-p2s1-p1-p5=1 [agent-p6 agent-p2-s1 agent-p1 agent-p5=1]
      agents-waiting-p6 [agent-p6]

      job-type-1 skill-1
      job-type-2 skill-2
      job-type-3 skill-3
      job-type-4 skill-4
      job-type-5=job-type-1 skill-5=skill-1

      job-1t #::specs.job{:id "1" :type job-type-1 :urgent true}
      job-2t #::specs.job{:id "2" :type job-type-2 :urgent true}
      job-3f #::specs.job{:id "3" :type job-type-3 :urgent false}
      job-4f #::specs.job{:id "4" :type job-type-4 :urgent false}
      job-5t #::specs.job{:id "5" :type job-type-5=job-type-1 :urgent true}

      empty-jobs []
      jobs-1t2t [job-1t job-2t]
      jobs-1t2t3f4f5t [job-1t job-2t job-3f job-4f job-5t]

      primary-skillset ::specs.agents/primary-skillset
      secondary-skillset ::specs.agents/secondary-skillset

      primary-true #::specs.matching-criteria{:skillset primary-skillset
                                              :urgent   true}
      primary-false #::specs.matching-criteria{:skillset primary-skillset
                                               :urgent   false}
      secondary-true #::specs.matching-criteria{:skillset secondary-skillset
                                                :urgent   true}
      secondary-false #::specs.matching-criteria{:skillset secondary-skillset
                                                 :urgent   false}

      order-of-priority [primary-true
                                              primary-false
                                              secondary-true
                                              secondary-false]]
  (facts "first-job-matching-skill-and-urgency"

         (fact "if jobs is empty, returns nil"
               (first-job-matching-skill-and-urgency empty-jobs true skill-1) => nil)
         (fact "if both skill and urgency do not appear in the jobs vector returns nil"
               (first-job-matching-skill-and-urgency jobs-1t2t false skill-6) => nil)
         (fact "if just skill or urgency appear in the same job returns nil"
               (first-job-matching-skill-and-urgency jobs-1t2t false skill-1) => nil
               (first-job-matching-skill-and-urgency jobs-1t2t true skill-3) => nil)
         (fact "if skill and urgency appear both in the jobs coll but not in the same job, returns nil"
               (first-job-matching-skill-and-urgency jobs-1t2t3f4f5t false skill-1) => nil
               (first-job-matching-skill-and-urgency jobs-1t2t3f4f5t true skill-3) => nil)
         (fact "if skill and urgency appear both in the same job, returns the first one"
               (first-job-matching-skill-and-urgency jobs-1t2t3f4f5t true skill-5=skill-1) => job-1t)
         (defspec runs-without-erros-for-all-inputs
                  fix/runs-for-each-prop-tests
                  (prop/for-all [jobs-with-matching-type (fix/gen-type-matching-jobs)]
                                (let [candidate-jobs-for-matching (rest jobs-with-matching-type)
                                      skill-and-urgency-to-be-matched (first jobs-with-matching-type)
                                      {skill  ::specs.job/type, urgent ::specs.job/urgent
                                       ;; if jobs-with-matching-type is empty, skill and urgency to be matched must have a value
                                       :or {skill "bills", urgent true}} skill-and-urgency-to-be-matched]
                                  (#'queues.logic.matcher/first-job-matching-skill-and-urgency candidate-jobs-for-matching urgent skill)
                                  ;; test focus on making sure it does not throws any erros so if it gets to this point always returns true
                                  true))))
  ;; TODO [QUESTION; TEST] is this use of let above a good way to write a test? At one side, it is concise and easy to modify. the name of the variables can help understand but at the same time the reader might still want to go back to the let many times

  (facts "first-job-matching-agent-on-specific-criteria"
         (fact "if jobs is EMPTY, returns nil"
               (first-job-matching-agent-on-specific-criteria empty-jobs
                                                              agent-p1
                                                              primary-true) => nil)
         (fact "if agent skillset being searched is EMPTY, returns nil"
               (first-job-matching-agent-on-specific-criteria jobs-1t2t
                                                              agent-p1
                                                              secondary-true) => nil)
         (fact "if agent HAS the skill to perform one of the jobs BUT NOT with the skillset being searched, returns nil"
               (first-job-matching-agent-on-specific-criteria jobs-1t2t
                                                              agent-p1-s6
                                                              secondary-true) => nil)
         (fact "if agent HAS the skill to perform one of the jobs with the skillset being searched, but urgency of this job and of the criteria DO NOT match, returns nil"
               (first-job-matching-agent-on-specific-criteria jobs-1t2t
                                                              agent-p1
                                                              primary-false) => nil)
         (fact "if agent HAS the skill to perform one of the jobs with the skillset being searched, and urgency of this job and of the criteria MATCH, returns the first job"
               (first-job-matching-agent-on-specific-criteria jobs-1t2t
                                                              agent-p12
                                                              primary-true) => job-1t))
  (facts "first-job-matching-agent"
         (fact "if jobs is EMPTY, returns nil"
               (first-job-matching-agent agent-p1
                                         empty-jobs
                                         order-of-priority) => nil)
         (fact "if agent skills DO NOT match ANY of the jobs, returns nil"
               (first-job-matching-agent agent-p6
                                         jobs-1t2t
                                         order-of-priority) => nil)
         (fact "if agent HAS the skill to perform one of the jobs returns the first job that matches accordingly to the highest priority criteria"
               (first-job-matching-agent agent-p12
                                         jobs-1t2t
                                         order-of-priority) => job-1t
               (first-job-matching-agent agent-p6-s4
                                         jobs-1t2t3f4f5t
                                         order-of-priority) => job-4f))
  (facts "agent-matches-job-type-through-skillset?"
         (fact "if agent and job-type DO NOT match returns nil"
               (agent-matches-job-type-through-skillset? agent-p1 job-type-2 primary-skillset) => false)
         (fact "if agent and job-type MATCH but NOT THROUGH the searched skillset returns nil"
               (agent-matches-job-type-through-skillset? agent-p1 job-type-1 secondary-skillset) => false)
         (fact "if agent and job-type MATCH THROUGH the searched skillset returns true"
               (agent-matches-job-type-through-skillset? agent-p1 job-type-1 primary-skillset) => true))
  (facts "first-agent-matching-job-type-through-skillset"
         (fact "if agents is EMPTY returns nil"
               (first-agent-matching-job-type-through-skillset job-type-1
                                                               empty-agents-waiting
                                                               primary-skillset) => nil)
         (fact "if agent HAS NO skill that matches the job-type returns nil"
               (first-agent-matching-job-type-through-skillset job-type-1
                                                               agents-waiting-p6
                                                               primary-skillset) => nil)
         (fact "if agent HAS the skill that matches the job-type but NOT IN the skillset being searched returns nil"
               (first-agent-matching-job-type-through-skillset job-type-1
                                                               agents-waiting-p1
                                                               secondary-skillset) => nil)
         (fact "if agent HAS the skill that matches the job-type IN the skillset being searched returns the agent"
               (first-agent-matching-job-type-through-skillset job-type-1
                                                               agents-waiting-p1
                                                               primary-skillset) => agent-p1)
         (fact "if MULTIPLE agents have the skill that matches the job-type IN the skillset being searched
         returns the first"
               (first-agent-matching-job-type-through-skillset job-type-1
                                                               agents-waiting-p1-p5=1
                                                               primary-skillset) => agent-p1)
         (fact "if MULTIPLE agents have the skill that matches the job-type IN the skillset being searched
         returns the first matching (not the first job)"
               (first-agent-matching-job-type-through-skillset job-type-1
                                                               agents-waiting-p6-p1-p5=1
                                                               primary-skillset) => agent-p1))
  (facts "first-agent-matching-job-type"
         (fact "If agents is EMPTY returns nil"
               (first-agent-matching-job-type job-type-1 empty-agents-waiting) => nil)
         (fact "If NO AGENT has a matching skill with job-type returns nil"
               (first-agent-matching-job-type job-type-1 agents-waiting-p6) => nil)
         (fact "If agent HAS a matching skill with job-type returns the agent"
               (first-agent-matching-job-type job-type-1 agents-waiting-p1) => agent-p1)
         (fact "If MULTIPLE agents have a matching skill with job-type returns the first agent"
               (first-agent-matching-job-type job-type-1 agents-waiting-p1-p5=1) => agent-p1)
         (fact "If MULTIPLE agents have a matching skill with job-type returns the first agent
         with matching skill to job-type but not the first agent in agents"
               (first-agent-matching-job-type job-type-1 agents-waiting-p6-p1-p5=1) => agent-p1)
         (fact "If MULTIPLE agents have a matching skill with job-type returns the first agent
         with matching skill in higher order of priority (primary > secondary)"
               (first-agent-matching-job-type job-type-1 agents-waiting-p6-p2s1-p1-p5=1) => agent-p1)))

(stest/instrument)