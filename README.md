# Boodschappie

Gedeelde boodschappenlijst voor Android. Meerdere lijsten + "Alles", indeling per categorie,
slimme suggesties tijdens het typen, aantallen en grammen, live gesynchroniseerd via Firebase.

**Installeren:** https://github.com/mijnmailfilter-cell/Boodschappen/releases/latest/download/Boodschappie.apk

## Eenmalige Firebase-setup
1. Firebase-project aanmaken, Android-app toevoegen met pakketnaam `nl.vliegendaapje.boodschappie`.
2. `google-services.json` downloaden en in `app/` zetten.
3. Authentication → Sign-in method → **Anoniem** aanzetten.
4. Firestore Database aanmaken (regio `europe-west4`), daarna bij **Regels** de inhoud van `firestore.rules` plakken en publiceren.
