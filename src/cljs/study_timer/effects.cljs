(ns study-timer.effects
  (:require [re-frame.core :as rf]))

(defn play
  ([{:keys [vco vca]} gain]
   (set! (.-value (.-gain vca)) gain))
  ([{:keys [vco vca] :as inst} gain frequency]
   (set! (.-value (.-frequency vco)) frequency)
   (play inst gain)))

(defn stop
  [inst]
  (play inst 0))

;; Todo: add multiple instruments
;; Todo: add custom instruments
(let [constructor (or js/window.AudioContext
                      js/window.webkitAudioContext)
      context (constructor.)
      vco (.createOscillator context)
      vca (.createGain context)
      inst {:vco vco :vca vca}
      default-options {:length 0.5 :frequency 400 :gain 1}]
  (stop inst)
  (.connect vco vca)
  (.connect vca (.-destination context))
  (.start vco)
  (rf/reg-fx
   :beep
   (fn beep
     [opts]
     (let [opts (merge default-options opts)]
       (play inst (:gain opts) (:frequency opts))
       (js/setTimeout
        (fn [] (stop inst))
        (* 1000 (:length opts)))))))
