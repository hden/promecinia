(ns promecinia.superlifter-test
  (:require [clojure.test :as t :refer [deftest testing is use-fixtures]]
            [com.walmartlabs.lacinia :refer [execute]]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
            [datascript.core :as d]
            [superlifter.api :as s]
            [promesa.core :as p]
            [promecinia.core :as core]
            [promecinia.core-test :as fixture]))

(def schema
  {:id
   {:db/cardinality  :db.cardinality/one
    :db/unique       :db.unique/identity}

   :appears_in
   {:db/cardinality  :db.cardinality/many}

   :character
   {:db/valueType    :db.type/ref}})

(def data
  [{:db/id       "luke"
    :id          1000
    :name        "Luke"
    :appears_in  [:NEWHOPE :EMPIRE :JEDI]
    :home_planet "Tatooine"}

   {:db/id       "lando"
    :id          2000
    :name        "Lando Calrissian"
    :cappears_in [:EMPIRE :JEDI]
    :home_planet "Socorro"}

   {:character "luke"
    :episode   :NEWHOPE
    :line      "But I was going into Tosche Station to pick up some power converters!"}

   {:character "lando"
    :episode   :EMPIRE
    :line      "Now, take the wookiee and Leia to my ship."}

   {:character "lando"
    :episode   :JEDI
    :line      "Home One, this is Gold Leader."}])

(def connection (d/create-conn schema))
(d/transact! connection data)

(def superlifter-args
  {:buckets {:default {:triggers {:interval {:interval 100}}}}
   :urania-opts {:env {:conn connection}}})

(def ^:private ^:dynamic *superlifter* nil)

(defn- manage-superlifter-buckets [f]
  (binding [*superlifter* (s/start! superlifter-args)]
    (try
      (f)
      (finally
        (s/stop! *superlifter*)))))

(use-fixtures :once manage-superlifter-buckets)

(s/def-superfetcher FetchHero [episode]
  (fn [coll {:keys [conn]}]
    (p/future
      (let [episodes (map :episode coll)
            results (into {} (d/q '[:find ?episode
                                          (pull ?e [:id
                                                    :name
                                                    :appears_in
                                                    :home_planet])
                                    :in $ [?episode ...]
                                    :where [?e :appears_in ?episode]]
                                   @conn episodes))]
        (map results episodes)))))

(s/def-superfetcher FetchQuote [character-id episode]
  (fn [coll {:keys [conn]}]
    (p/future
      (let [tuples (map (fn [{:keys [character-id episode]}] [character-id episode]) coll)
            results (into {}
                          (map (fn [[character-id episode quote]]
                                 [[character-id episode] quote]))
                          (d/q '[:find ?character-id
                                       ?episode
                                       (distinct ?s)
                                 :in $ [[?character-id ?episode] ...]
                                 :where [?c :id ?character-id]
                                        [?q :character ?c]
                                        [?q :episode ?episode]
                                        [?q :line ?s]]
                                @conn tuples))]
        (map results tuples)))))

(s/def-superfetcher FetchDroid [id]
  (fn [coll _]
    (-> (p/future
          (let [ids (map :id coll)]
            (throw (ex-info
                     "These aren’t the droids you’re looking for."
                     {:ids ids}))))
      (p/catch core/show-stack-traces))))

(def fetch-hero ->FetchHero)
(def fetch-quote ->FetchQuote)
(def fetch-droid ->FetchDroid)

(defn get-hero-resolver [context arguments _]
  (let [{:keys [superlifter]} context
        {:keys [episode]} arguments]
    (-> (s/with-superlifter superlifter
          (s/enqueue! (fetch-hero episode)))
      (core/attach
        {:warning {:message "Try not. Do or do not. There is no try."}
         :context {:episode episode}}))))

(defn fetch-quote-resolver [context _ value]
  (let [{:keys [superlifter episode]} context
        {:keys [id]} value]
    (s/with-superlifter superlifter
      (s/enqueue! (fetch-quote id episode)))))

(defn fetch-droid-resolver [context arguments _]
  (let [{:keys [superlifter]} context
        {:keys [id]} arguments]
    (s/with-superlifter superlifter
      (s/enqueue! (fetch-droid id)))))

(def star-wars-schema
  (-> fixture/star-wars-schema-edn
    (attach-resolvers {:get-hero get-hero-resolver
                       :get-droid fetch-droid-resolver
                       :get-quotes fetch-quote-resolver})
    schema/compile))

(deftest get-hero-test
  (let [result (execute star-wars-schema fixture/hero-query nil {:superlifter *superlifter*})]
    (testing "on-deliver"
      (is (= "Luke" (get-in result [:data :hero :name]))))

    (testing "attach-context"
      (is (= ["But I was going into Tosche Station to pick up some power converters!"]
             (get-in result [:data :hero :quotes]))))

    (testing "attach-warning"
      (is (= "Try not. Do or do not. There is no try."
             (-> (get-in result [:extensions :warnings])
               first
               :message))))))

(deftest get-droid-test
  (testing "hide stack trace"
    (let [result (execute star-wars-schema fixture/droid-query nil {:superlifter *superlifter*})]
      (is (= "These aren’t the droids you’re looking for."
             (-> (get-in result [:errors])
               first
               :message))))))
