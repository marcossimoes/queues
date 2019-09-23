(ns queues.json-converter
  (:require [cheshire.core :as che]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [queues.specs.events :as specs.events]
            [queues.specs.jobs-assigned :as specs.jobs-assigned]
            [queues.specs.json-events :as specs.json-events]))

(defn- ns-kwd-key-from-json-key
  [nmspc js-kw]
  (->> js-kw
       (#(str/replace % #"_" "-"))
       (keyword (str "queues.specs." nmspc))))

(s/fdef ns-kwd-key-from-json-key
        :args (s/cat :nmspc string?
                     :js-kw string?)
        :ret keyword?)

(defn- clj-event-payload-from-namespace-and-json-event-payload
  [nmspc js-payload]
  (reduce-kv
    (fn [clj-payload json-key json-value]
      (let [clj-key (ns-kwd-key-from-json-key nmspc json-key)
            clj-value json-value]
        (assoc clj-payload clj-key clj-value)))
    {}
    js-payload))

(s/fdef clj-event-payload-from-namespace-and-json-event-payload
        :args (s/cat :nmspc string?
                     :js-payload map?)
        :ret keyword?)

;; TODO [QUESTION; ARCH] this hardcoded REGEX bellow seem weired. Should it be moved to a config?

(defn- ns-from-js-event-type
  [js-event-type]
  (str/replace js-event-type #"new_agent|new_job|job_request" {"new_agent" "agents" "new_job" "job" "job_request" "job-request"}))

(s/fdef ns-from-js-event-type
        :args (s/cat :js-event-type string?)
        :ret string?)

(defn- clj-event-payload-from-json-type-and-payload
  [js-event-type js-event-payload]
  (let [event-namespace (ns-from-js-event-type js-event-type)]
    (clj-event-payload-from-namespace-and-json-event-payload event-namespace
                                                             js-event-payload)))

(s/fdef clj-event-payload-from-json-type-and-payload
        :args (s/cat :js-event-type string?
                     :js-event-payload map?)
        :ret ::specs.events/event)

(defn- clj-event-from-json-event
  [js-event]
  (let [[js-event-type js-event-payload] (first js-event)
        clj-event-type (ns-kwd-key-from-json-key "events" js-event-type)
        clj-event-payload (clj-event-payload-from-json-type-and-payload js-event-type js-event-payload)]
    (->> {clj-event-type clj-event-payload}
         (s/conform ::specs.events/event)
         (second))))

(s/fdef clj-event-from-json-event
        :args (s/cat :js-event map?)
        :ret ::specs.events/event)

(defn- clj-events-with-converted-json-event
  [clj-events js-event]
  (conj clj-events (clj-event-from-json-event js-event)))

(s/fdef clj-events-with-converted-json-event
        :args (s/cat :clj-events ::specs.events/events
                     :js-event map?)
        :ret ::specs.events/events)

;; TODO [IMPROVE, TEST] have a runtime check for the js-events after the file content string has been parsed
;; we should through an exception/error and ask the user to provide a new file, or check the file
;; for corruption

(defn- clj-events-from-json-events
  [json-events]
  (let [clj-events []]
    (reduce clj-events-with-converted-json-event
            clj-events
            json-events)))

(s/fdef clj-events-from-json-events
        :args (s/cat :json-events ::specs.json-events/json-events)
        :ret ::specs.events/events)

(defn json-event-from-json-event-str
  [json-event-str]
  (che/parse-string json-event-str))

(s/fdef json-event-from-json-event-str
        :args (s/cat :json-event-str string?)
        :ret ::specs.json-events/json-event)

(defn clj-event-from-json-event-str
  [json-event-str]
  (-> json-event-str
      (json-event-from-json-event-str)
      (clj-event-from-json-event)))

(s/fdef clj-event-from-json-event-str
        :args (s/cat :json-event-str string?)
        :ret ::specs.events/event)

(defn- json-events-vec-from-json-events-str
  [json-events-str]
  (if-let [json-events-vec (che/parse-string json-events-str)]
    json-events-vec
    []))

(s/fdef json-events-vec-from-json-events-str
        :args (s/cat :json-events-str string?)
        :ret (s/or :no-events nil?
                   :json-events ::specs.json-events/json-events))

(defn clj-events-from-json-events-str
  [json-events-str]
  (when-let [json-events-vec (json-events-vec-from-json-events-str json-events-str)]
    (clj-events-from-json-events json-events-vec)))

(s/fdef clj-events-from-json-events-str
        :args (s/cat :json-events-str string?)
        :ret (s/or :events ::specs.events/events
                   :no-events nil?))

(defn- json-key-from-clj-ns-kwd-key
  [clj-ns-kwd-key]
  (->> clj-ns-kwd-key
       (name)
       (#(str/replace % #"-" "_"))))

(s/fdef json-key-from-clj-ns-kwd-key
        :args (s/cat :clj-ns-kwd-key keyword?)
        :ret string?)

(defn json-events-str-formatted-from-clj-events
  [clj-events]
  (che/generate-string clj-events {:key-fn json-key-from-clj-ns-kwd-key :pretty true}))

(s/fdef json-events-str-formatted-from-clj-events
        :args (s/cat :clj-events ::specs.jobs-assigned/jobs-assigned)
        :ret string?)

;; TODO [IMPROVE] Have two specs for events: input-events and output-events