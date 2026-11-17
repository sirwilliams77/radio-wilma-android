# Radio Wilma na Androida

Aplikacja radia internetowego na telefon w stylu serii **Radio Wilma 4.0** (ESP32-S3): czarne tło, niebieskie ramki, duży zegar, pogoda, wirujący winyl, equalizer, ulubione stacje, wyszukiwarka w katalogu radio-browser.info i motywy kolorów takie same jak w panelu WWW radia.

Wersja **1.1.0**. Autor: **Sir Williams**. Licencja: **GNU GPL v3** (plik `LICENSE`).

> Kod, dokumentacja i makieta powstały z pomocą asystenta AI (Claude, Anthropic); całość została zaprojektowana, przetestowana i jest wydawana przez autora.

> Zachowanie aplikacji (zwłaszcza kafelka na ekranie blokady) może się różnić zależnie od telefonu i nakładki producenta.

## Co potrafi

- Odtwarzanie strumieni radiowych w tle (AndroidX Media3). Kafelek na ekranie blokady z logo Radio Wilma ma przyciski poprzedniej i następnej stacji (ulubione), pauzy i zamknięcia.
- **Wyjście z aplikacji:** przycisk zasilania na ekranie głównym i w Ustawieniach zatrzymuje radio i zamyka aplikację; zamknięcie ze switchera też zatrzymuje radio.
- Dotknięcie kafelka „Stacja radiowa” otwiera listę ulubionych do szybkiego wyboru.
- Układ pionowy i poziomy.
- **Ekran odtwarzacza** jak w radiu Wilma: zegar i data, temperatura z miastem i ikoną pogody, nazwa stacji, wykonawca i tytuł z danych strumienia (ICY), winyl, equalizer, głośność, poprzednia i następna stacja, pasek informacji (numer na liście, jakość, kodek, gatunek, strefa czasowa).
- **Stacje:** lista ulubionych, zmiana kolejności strzałkami, usuwanie, **dodawanie stacji z adresu URL** (np. adres strumienia z radia Wilma 4.0).
- **Logo stacji:** dotknij kółka przy stacji, aby wgrać obraz z telefonu albo pobrać logo z katalogu (też hurtowo: „Pobierz brakujące logo”). Logo pojawia się na liście, na ekranie głównym i na kafelku ekranu blokady.
- **Szukaj:** katalog radio-browser.info (tylko sprawdzone, działające strumienie, bez HLS i playlist, jak w radiu Wilma 4.0), filtry kraju, gatunku i minimalnej jakości; „Graj” albo „Dodaj do ulubionych”.
- **Wygląd:** pięć motywów (Niebieski, Bursztynowy, Zielony retro, Biało-szary, Czerwony), jak w panelu radia.
- **Pogoda:** wpisz miejscowość, dane z Open-Meteo, odświeżanie co 20 minut.
- **Wyłącznik czasowy:** 15, 30, 45, 60, 90 lub 120 minut.

Ograniczenia pierwszej wersji:

- Equalizer jest **animowany dla ozdoby**: Android nie daje widma strumienia bez zgody na mikrofon, więc słupki nie odzwierciedlają dokładnie dźwięku.
- Logo z katalogu bywa nieaktualne lub go brakuje (wtedy zostaje kółko z inicjałami albo logo wgrane ręcznie). Zmiana logo grającej właśnie stacji pojawia się na ekranie blokady po jej ponownym włączeniu.
- Stacje, które nie wysyłają tytułów utworów w strumieniu, pokazują „Brak informacji o utworze”.
- Nie ma jeszcze sterowania radiem Wilma przez sieć, budzika ani importu i eksportu listy stacji. To plan na kolejne wersje.
- Edycja pojedynczych kolorów (poza motywami) jeszcze nie jest dostępna.

## Jak zbudować i zainstalować

Potrzebujesz **Android Studio** (darmowe, https://developer.android.com/studio) z zainstalowanym Android SDK 34.

1. Uruchom Android Studio i wybierz **Open**, a potem wskaż ten folder (`Radio Wilma Android`). Przy pierwszym otwarciu pobiera Gradle i biblioteki, więc potrwa kilka minut.
2. Jeśli Android Studio zapyta o aktualizację wtyczki Gradle, możesz odmówić albo zaakceptować (projekt używa Gradle 8.9, AGP 8.5.2, Kotlin 2.0.20).
3. **Plik APK:** menu **Build → Generate App Bundles or APKs → Generate APKs** (nie używaj samego „Run”, bo ten plik ma znacznik testowy i nie da się go zainstalować z instalatora). Gotowy plik znajdziesz w `app/build/outputs/apk/debug/app-debug.apk`.
4. **Instalacja na telefonie** (dwa sposoby):
   - Kablem: na telefonie włącz *Opcje programisty* i *Debugowanie USB*, podłącz telefon, w Android Studio wybierz go na liście urządzeń i kliknij **Run**.
   - Ręcznie: skopiuj `app-debug.apk` na telefon i otwórz go. Android poprosi o zgodę na instalację z nieznanych źródeł dla aplikacji, z której otwierasz plik.
5. Przy pierwszym uruchomieniu zezwól na powiadomienia (Android 13 i nowszy), bo w nich są przyciski odtwarzania.

## Pierwsze kroki w aplikacji

1. Wejdź w **Szukaj**, a wyniki dla Polski pojawią się od razu. Wpisz nazwę, np. „jazz”, i zatwierdź.
2. Przy wybranej stacji dotknij **Dodaj do ulubionych** albo **Graj**.
3. W **Ustawienia → Pogoda** wpisz swoją miejscowość, żeby zobaczyć temperaturę i ikonę.
4. W **Ustawienia → Wygląd** wybierz motyw.

## Struktura projektu

| Plik | Zawartość |
|---|---|
| `app/src/main/java/.../MainActivity.kt` | okno aplikacji, dolna nawigacja |
| `PlaybackService.kt` | odtwarzanie w tle (Media3, powiadomienie) |
| `PlayerController.kt` | połączenie ekranu z odtwarzaczem, odczyt tytułu utworu |
| `MainViewModel.kt` | ulubione, wyszukiwanie, pogoda, motyw, wyłącznik czasowy |
| `RadioBrowserApi.kt`, `WeatherApi.kt` | katalog stacji (wyszukiwanie, logo) i pogoda |
| `LogoStore.kt` | logo stacji zapisane w aplikacji |
| `ui/` | ekrany Compose: odtwarzacz, stacje, szukaj, ustawienia, motywy |

## Licencje

Projekt jest wolnym oprogramowaniem na licencji **GNU GPL v3** (pełny tekst w pliku `LICENSE`), na bazie projektu Radio Wilma (Sir Williams), © 2026.
Możesz go używać, zmieniać i rozpowszechniać, ale zmodyfikowaną wersję trzeba udostępnić na tej samej licencji wraz z kodem źródłowym.
Projekt jest dostarczany bez gwarancji, używasz go na własną odpowiedzialność.

Autor i właściciel projektu: Sir Williams. Kod, dokumentacja i makieta powstały z pomocą asystenta AI (Claude, Anthropic); całość została zaprojektowana, przetestowana i jest wydawana przez autora.

Elementy zewnętrzne (użyte jako biblioteki albo dane):

| Element | Licencja |
|---|---|
| AndroidX Media3, Jetpack Compose, AndroidX | Apache-2.0 |
| Kotlin | Apache-2.0 |
| OkHttp (Square) | Apache-2.0 |
| Dane pogodowe Open-Meteo.com | CC BY 4.0 (użytek niekomercyjny) |
| Katalog stacji radio-browser.info | dane społecznościowe, bez gwarancji; nazwy i logotypy stacji należą do ich nadawców |

Biblioteki Apache-2.0 są zgodne z GPL v3. Nazwy stacji radiowych są używane wyłącznie do ich identyfikacji.
