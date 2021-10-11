# promecinia

Use [promesa promises](https://github.com/funcool/promesa) as lacinia ResolverResults.

## Installation

`[com.github.hden/promecinia "0.1.0-SNAPSHOT"]`

## Usage

### Plain Usage

Just return a promise within a resolver.

```clj
(require '[promecinia.core :as core])
(require '[promesa.core :as p])

(defn get-hero [_ arguments _]
  (let [{:keys [episode]} arguments]
    (-> (p/future
          (Thread/sleep 100)
          (if (= episode :NEWHOPE)
            {:id 1000
             :name "Luke"
             :home_planet "Tatooine"
             :appears_in ["NEWHOPE" "EMPIRE" "JEDI"]}
            {:id 2000
             :name "Lando Calrissian"
             :home_planet "Socorro"
             :appears_in ["EMPIRE" "JEDI"]}))
      (core/attach
        {:warning {:message "Try not. Do or do not. There is no try."}
         :context {:episode episode}}))))
```

### Superlifter

Since Lacinia can now work directly with promises, there is no need to bridge functions under `superlifter.lacinia` namespace.

```clj
(require '[superlifter.api :as s]) ;; not superlifter.lacinia!

(s/def-superfetcher FetchHero [episode]
  (fn [coll {:keys [conn]}]
    (-> (p/future
         (let [episodes (map :episode coll)]
           (find-heros-by-episodes episodes)))
      (p/catch (fn [ex]
                 (map (constantly {:message "My error message."}) coll))))))

(defn get-hero-resolver [context arguments _]
  (let [{:keys [superlifter]} context
        {:keys [episode]} arguments]
    (-> (s/with-superlifter superlifter
          (s/enqueue! (fetch-hero episode)))
      (core/attach
        {:warning {:message "Try not. Do or do not. There is no try."}
         :context {:episode episode}}))))
```

### Error handling

By default, promecinia hides error messages as internal server errors.
To customize GraphQL errors, use `com.walmartlabs.lacinia.resolve/with-error`.

```clj
(require '[com.walmartlabs.lacinia.resolve :as resolve])

(-> (p/future
      (throw some-internal-error))
  (p/catch (fn [ex]
             (resolve/with-error nil {:message "These aren’t the droids you’re looking for."}))))
```

## License

Copyright © 2021 Haokang Den

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
