# OS2sofd Leder

Lederportalen til [OS2sofd](https://github.com/OS2sofd/os2sofd). En selvbetjeningsportal, hvor ledere, stedfortrædere og andre udpegede personer vedligeholder udvalgte medarbejderdata decentralt. Portalen ejer stort set ingen data selv. Alt om personer, enheder, tilhørsforhold, ledere, stedfortrædere og funktioner ligger i OS2sofd Core og tilgås via dets manager-API.

Produktbeskrivelse, funktioner og priser findes på [sofd.io/lederportal](https://www.sofd.io/lederportal/). En beskrivelse af roller og moduler set fra brugerens side findes i [documentation/documentation.md](documentation/documentation.md).

## Moduler

Hvert modul kan slås til og fra pr. installation under `manager.modules.*` i konfigurationen.

- Kontogodkendelse
- Tilhørsforhold
- Pausemarkering
- Stedfortrædere
- Medarbejderfunktioner
- Lederopmærkning

## Teknik

Spring Boot på Java 25, bygget med Maven. Brugergrænsefladen er Thymeleaf. Login sker via SAML, og roller kommer dels fra OS2sofd Core (leder og stedfortræder), dels fra SAML-tokenet (administrator, lederopmærker, pauseopmærker og læseadgang).

Portalen har kun en enkelt egen tabel til redigerbare hjælpetekster. Databasen er MySQL og migreres med Flyway ved opstart.

## Byg og kør

```
mvn clean package
mvn spring-boot:run
```

Applikationen læser konfiguration fra `config/application.properties`. I det offentlige repo er alle værdier i filen tømt, så den skal udfyldes før første opstart. Som minimum skal følgende sættes.

| Nøgle | Betydning |
|---|---|
| `spring.datasource.*` | Forbindelse til MySQL-databasen |
| `di.saml.*` | SAML-opsætning, herunder keystore og IdP-metadata |
| `server.ssl.*` | Keystore til HTTPS |
| `manager.sofdBaseUrl` | URL til OS2sofd Core |
| `manager.sofdApiKey` | API-nøgle til manager-API'et i OS2sofd Core |
| `manager.modules.*` | Hvilke moduler der skal være slået til |

Keystores er ikke en del af repoet. Opsætning af ADFS som IdP er beskrevet i [documentation/ADFSSaml.txt](documentation/ADFSSaml.txt).

## Licens

Mozilla Public License 2.0. Se [LICENSE](LICENSE).
