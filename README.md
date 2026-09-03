# Gestione Chiamate (Android)

App Android per la gestione di chiamate, SMS e rubrica.

## Funzioni
- **Preferiti**: 6 numeri rapidi, si impostano toccando uno slot vuoto e scegliendo un contatto.
- **Tastierino**: composizione numero e chiamata diretta.
- **Rubrica**: elenco contatti con chiamata rapida.
- **Chiamate**: registro chiamate recenti.
- **SMS**: elenco messaggi ricevuti.
- **Tastierino sempre disponibile durante la chiamata**: se l'app viene impostata come
  app "Telefono" predefinita (menu ⋮ → "Imposta come app Telefono predefinita"), la
  schermata di chiamata mostra sempre il tastierino DTMF, utilizzabile per tutta la
  durata della chiamata. Se non viene impostata come predefinita, un piccolo tastierino
  flottante compare comunque sopra le altre app durante la chiamata (richiede il
  permesso "Consenti tastierino sopra le altre app", nel menu ⋮).

## Permessi richiesti
Rubrica, telefono, registro chiamate, SMS. Vengono chiesti al primo avvio.

## Build con Android Studio
1. Apri la cartella del progetto con Android Studio (File → Open).
2. Lascia sincronizzare Gradle.
3. Esegui su un dispositivo/emulatore con Run ▶, oppure Build → Build APK(s).

## Build da riga di comando (es. Termux)
Vedi le istruzioni fornite a parte per generare l'APK direttamente su Termux
oppure per compilare su GitHub Actions dopo il push.

## Note tecniche
- `minSdk 24`, `targetSdk/compileSdk 34`, Kotlin.
- Il tastierino durante la chiamata reale è realizzato con `InCallService`
  (richiede il ruolo "Telefono predefinito").
- Il tastierino flottante di riserva usa `SYSTEM_ALERT_WINDOW` e un
  `Foreground Service`.
