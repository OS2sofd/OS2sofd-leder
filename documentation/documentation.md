# OS2sofd Leder-UI - Overblik

## Hvad er det?

OS2sofd Leder-UI er en selvbetjeningsportal til decentralt vedligehold af udvalgte medarbejderdata. Ud over ledere og deres stedfortrædere kan andre personer i organisationen udpeges til at løse opgaver i portalen. Portalen er et supplement til OS2sofd Core, som ligger under og holder selve data. Ændringer her slår igennem i OS2sofd Core.

## Log ind

Login sker via SAML/SSO. Har du flere roller, vælger du ved login, hvilken du vil arbejde i. Du kan skifte rolle undervejs via **"Log ind som"** i toppen.

## Roller

| Rolle | Adgang |
|---|---|
| **Leder** | Administrerer medarbejdere i egne afdelinger |
| **Stedfortræder** | Handler på en leders vegne, evt. afgrænset til udvalgte afdelinger |
| **Lederopmærker** | Udpeger ledere for afdelinger |
| **Pauseopmærker** | Sætter og fjerner pausemarkeringer |
| **Læseadgang** | Kan se, ikke ændre |
| **Administrator** | Fuld adgang |

Roller kan være afgrænsede til bestemte afdelinger.

---

## Moduler

De enkelte moduler kan slås til og fra pr. installation. Menuen viser kun det, din rolle og konfigurationen giver adgang til.

### Kontogodkendelse
Liste over konti, der afventer godkendelse. Lederen eller stedfortræderen godkender kontoen, før medarbejderen kan bruge den.

### Tilhørsforhold
Oversigt over medarbejderes ansættelser (stilling, afdeling, periode). Kun tilhørsforhold, der er oprettet manuelt i OS2sofd, kan redigeres; tilhørsforhold, der kommer fra et lønsystem eller anden kilde, er skrivebeskyttede. På de manuelt oprettede kan stillingsbetegnelse, alternativ stillingsbetegnelse, start- og slutdato samt intern reference rettes. Hvis notifikation er slået til, kræves en begrundelse ved ændring.

### Pausemarkering
Markér en medarbejder som værende på pause (orlov, skoleophold, inaktiv, andet) med startdato, slutdato og evt. uddybning. Der kan vælges, om medarbejderens IdM-processer skal undtages, og om AD-konti skal have udløb sat i perioden.

### Stedfortrædere
Lederen udpeger en eller flere stedfortrædere og kan afgrænse dem til udvalgte afdelinger.

### Medarbejderfunktioner
Administration af særlige funktioner ud over den almindelige stilling (fx tillidsrepræsentant, brandansvarlig). Funktioner tildeles en medarbejder med start- og slutdato samt eventuelle supplerende felter. Menuen viser både **"Alle medarbejderfunktioner"** og en opdeling efter kategorier defineret i konfigurationen.

### Lederopmærkning
Udpegning af leder for en afdeling. En leder, der sættes på en afdeling, arves af underafdelinger, indtil en underafdeling selv har en leder. En manuel udpegning kan fjernes, hvorefter afdelingen arver fra overliggende niveau.

