package com.paolo.gestionechiamate

object InfoNumero {

    // Il prefisso identifica il distretto telefonico, non sempre il singolo comune.
    private val distretti = mapOf(
        "010" to "Genova", "011" to "Torino", "0121" to "Pinerolo", "0122" to "Susa",
        "0123" to "Lanzo Torinese", "0124" to "Rivarolo Canavese", "0125" to "Ivrea",
        "0131" to "Alessandria", "0141" to "Asti", "015" to "Biella", "0161" to "Vercelli",
        "0165" to "Aosta", "0171" to "Cuneo", "0185" to "Chiavari", "0187" to "La Spezia",
        "019" to "Savona", "02" to "Milano", "030" to "Brescia", "031" to "Como",
        "0321" to "Novara", "0322" to "Arona", "0323" to "Verbania", "0324" to "Domodossola",
        "0331" to "Busto Arsizio", "0332" to "Varese", "0341" to "Lecco", "0342" to "Sondrio",
        "0343" to "Chiavenna", "035" to "Bergamo", "0362" to "Monza e Brianza",
        "0363" to "Treviglio", "0371" to "Lodi", "0372" to "Cremona", "0376" to "Mantova",
        "0381" to "Vigevano", "0382" to "Pavia", "0383" to "Voghera", "039" to "Monza",
        "040" to "Trieste", "041" to "Venezia", "0421" to "San Donà di Piave",
        "0422" to "Treviso", "0423" to "Montebelluna", "0424" to "Bassano del Grappa",
        "0425" to "Rovigo", "0431" to "Cervignano del Friuli", "0432" to "Udine",
        "0434" to "Pordenone", "0435" to "Pieve di Cadore", "0436" to "Cortina d'Ampezzo",
        "0437" to "Belluno", "0438" to "Conegliano", "0444" to "Vicenza", "0445" to "Schio",
        "045" to "Verona", "0461" to "Trento", "0462" to "Cavalese", "0464" to "Rovereto",
        "0471" to "Bolzano", "0472" to "Bressanone", "0473" to "Merano", "0474" to "Brunico",
        "0481" to "Gorizia", "049" to "Padova", "050" to "Pisa", "051" to "Bologna",
        "0521" to "Parma", "0522" to "Reggio Emilia", "0523" to "Piacenza", "0524" to "Fidenza",
        "0525" to "Fornovo di Taro", "0532" to "Ferrara", "0535" to "Mirandola",
        "0536" to "Sassuolo", "0541" to "Rimini", "0542" to "Imola", "0543" to "Forlì",
        "0544" to "Ravenna", "0545" to "Lugo", "0546" to "Faenza", "055" to "Firenze",
        "0564" to "Grosseto", "0565" to "Piombino", "0571" to "Empoli", "0572" to "Montecatini Terme",
        "0573" to "Pistoia", "0574" to "Prato", "0575" to "Arezzo", "0577" to "Siena",
        "0578" to "Montepulciano", "0583" to "Lucca", "0584" to "Viareggio", "0585" to "Massa",
        "0586" to "Livorno", "0587" to "Pontedera", "0588" to "Volterra", "059" to "Modena",
        "06" to "Roma", "070" to "Cagliari", "071" to "Ancona", "0721" to "Pesaro",
        "0722" to "Urbino", "0731" to "Jesi", "0732" to "Fabriano", "0733" to "Macerata",
        "0734" to "Fermo", "0735" to "San Benedetto del Tronto", "0736" to "Ascoli Piceno",
        "0742" to "Foligno", "0743" to "Spoleto", "0744" to "Terni", "075" to "Perugia",
        "0761" to "Viterbo", "0763" to "Orvieto", "0765" to "Poggio Mirteto", "0766" to "Civitavecchia",
        "0771" to "Formia", "0773" to "Latina", "0774" to "Tivoli", "0775" to "Frosinone",
        "0776" to "Cassino", "0789" to "Olbia", "079" to "Sassari", "080" to "Bari",
        "081" to "Napoli", "0823" to "Caserta", "0824" to "Benevento", "0825" to "Avellino",
        "0831" to "Brindisi", "0832" to "Lecce", "0833" to "Gallipoli", "0835" to "Matera",
        "0841" to "Potenza", "085" to "Pescara", "0861" to "Teramo", "0862" to "L'Aquila",
        "0863" to "Avezzano", "0864" to "Sulmona", "0871" to "Chieti", "0872" to "Lanciano",
        "0874" to "Campobasso", "0875" to "Termoli", "0881" to "Foggia", "0883" to "Andria",
        "0884" to "Manfredonia", "089" to "Salerno", "090" to "Messina", "091" to "Palermo",
        "0921" to "Cefalù", "0922" to "Agrigento", "0923" to "Trapani", "0924" to "Alcamo",
        "0925" to "Sciacca", "0931" to "Siracusa", "0932" to "Ragusa", "0933" to "Caltagirone",
        "0934" to "Caltanissetta", "0935" to "Enna", "0941" to "Patti", "0942" to "Taormina",
        "095" to "Catania", "0961" to "Catanzaro", "0962" to "Crotone", "0963" to "Vibo Valentia",
        "0964" to "Locri", "0965" to "Reggio Calabria", "0966" to "Palmi", "0971" to "Potenza",
        "0972" to "Melfi", "0973" to "Lagonegro", "0974" to "Vallo della Lucania",
        "0975" to "Sala Consilina", "0976" to "Muro Lucano", "0981" to "Castrovillari",
        "0982" to "Paola", "0983" to "Rossano", "0984" to "Cosenza", "099" to "Taranto"
    )

    fun descrizione(numero: String): String {
        val nazionale = numeroNazionale(numero) ?: return "Numero internazionale"
        if (nazionale.isBlank()) return "Numero privato o sconosciuto"
        if (nazionale.startsWith('3')) return "Cellulare"
        if (!nazionale.startsWith('0')) return "Numero speciale"
        val localita = distretti.entries
            .sortedByDescending { it.key.length }
            .firstOrNull { nazionale.startsWith(it.key) }
            ?.value
        return if (localita != null) "Rete fissa • $localita" else "Rete fissa • Zona non riconosciuta"
    }

    fun isCellulareItaliano(numero: String): Boolean =
        numeroNazionale(numero)?.matches(Regex("3\\d{8,10}")) == true

    private fun numeroNazionale(numero: String): String? {
        val originale = numero.trim()
        if (originale.isBlank() || originale == "-1" || originale == "-2") return ""
        val cifre = originale.filter { it.isDigit() }
        return when {
            originale.startsWith("+") && !cifre.startsWith("39") -> null
            originale.startsWith("00") && !cifre.startsWith("0039") -> null
            cifre.startsWith("0039") -> cifre.drop(4)
            cifre.startsWith("39") && cifre.length > 10 -> cifre.drop(2)
            else -> cifre
        }
    }
}
