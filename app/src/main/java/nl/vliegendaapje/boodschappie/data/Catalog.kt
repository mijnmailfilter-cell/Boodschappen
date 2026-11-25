package nl.vliegendaapje.boodschappie.data

/**
 * Ingebouwde lijst met veelvoorkomende boodschappen.
 * "Naam@500" betekent: standaard in grammen, 500 g.
 */
object Catalog {
    data class Product(val name: String, val category: Int, val unit: String, val qty: Long)

    private val raw = mapOf(
        1 to """Appels, Handappels, Peren, Bananen, Sinaasappels, Handsinaasappels, Mandarijnen, Citroenen, Limoenen,
            Druiven, Witte druiven, Blauwe druiven, Aardbeien, Blauwe bessen, Frambozen, Bramen, Kiwi's, Mango, Ananas,
            Meloen, Watermeloen, Avocado, Granaatappel, Perziken, Nectarines, Pruimen, Kersen, Dadels, Vijgen, Kokosnoot,
            Rabarber, Tomaten, Trostomaten, Cherrytomaatjes, Snoeptomaatjes, Komkommer, Paprika, Rode paprika, Gele paprika,
            Groene paprika, Puntpaprika, Courgette, Aubergine, Uien, Rode uien, Sjalotten, Knoflook, Gember, Prei,
            Wortels, Winterpeen, Aardappelen@1500, Krieltjes@750, Kruimige aardappelen@1500, Vastkokende aardappelen@1500,
            Zoete aardappel, Broccoli, Bloemkool, Spinazie@300, Sla, IJsbergsla, Rucola, Veldsla, Andijvie@500,
            Boerenkool@500, Zuurkool@500, Witlof, Spruitjes@500, Sperziebonen@300, Snijbonen@300, Peultjes@200,
            Champignons@250, Kastanjechampignons@250, Oesterzwammen@200, Bleekselderij, Knolselderij, Venkel, Radijs,
            Rode bieten, Pompoen, Pastinaak, Asperges@500, Groene asperges, Lente-uitjes, Bosui, Rode peper, Pepers,
            Taugé@200, Paksoi, Rode kool, Witte kool, Spitskool, Chinese kool, Maiskolven, Verse basilicum,
            Verse peterselie, Verse koriander, Verse munt, Bieslook, Verse dille, Soepgroente, Roerbakgroente,
            Wokgroente, Salademix, Gesneden groente, Fruitsalade, Gesneden fruit""",
        2 to """Brood, Volkorenbrood, Witbrood, Bruinbrood, Meergranenbrood, Speltbrood, Tijgerbrood, Casino brood,
            Stokbrood, Afbakbroodjes, Pistoletjes, Bolletjes, Krentenbollen, Croissants, Ciabatta, Focaccia, Bagels,
            Wraps, Tortilla's, Pitabroodjes, Naanbrood, Beschuit, Crackers, Knäckebröd, Ontbijtkoek, Krentenbrood,
            Rozijnenbrood, Suikerbrood, Hamburgerbroodjes, Hotdogbroodjes, Toastbrood, Pannenkoeken, Poffertjes,
            Appeltaart, Taart, Muffins, Donuts, Paneermeel@200""",
        3 to """Kipfilet@500, Kippendijen@500, Kippenpoten, Kipdrumsticks, Kippenvleugels, Hele kip, Kipstukjes@400,
            Kipgehakt@500, Gehakt@500, Rundergehakt@500, Half-om-half gehakt@500, Biefstuk, Entrecote, Ossenhaas,
            Riblappen@500, Sukadelappen@500, Stoofvlees@750, Hamlappen, Varkenshaas, Speklappen, Spekjes@200,
            Ontbijtspek, Rookworst, Braadworst, Saucijzen, Slavinken, Hamburgers, Gehaktballen, Shoarma@500, Gyros@500,
            Kipschnitzel, Schnitzel, Karbonades, Spareribs, Saté, Lamskoteletten, Kalkoenfilet@500, Zalm, Zalmfilet,
            Gerookte zalm@100, Kabeljauw, Pangasius, Tilapia, Tonijnsteak, Garnalen@200, Scampi's@200, Mosselen@1000,
            Haring, Makreel, Kibbeling, Vissticks, Vegaburgers, Vegetarische balletjes, Vegetarisch gehakt@300,
            Vegetarische kipstukjes@200, Tofu, Tempeh, Falafel""",
        4 to """Kaas, Jonge kaas, Belegen kaas, Oude kaas, Geraspte kaas@150, Plakken kaas, Smeerkaas, Roomkaas,
            Mozzarella, Burrata, Parmezaanse kaas, Feta, Geitenkaas, Brie, Camembert, Blauwe kaas, Gorgonzola,
            Mascarpone, Ricotta, Halloumi, Cheddar, Ham@150, Beenham@150, Kipfilet beleg@150, Rookvlees@100,
            Salami@100, Chorizo, Filet americain@150, Leverworst, Grillworst, Boterhamworst, Cervelaat, Rosbief@100,
            Paté, Prosciutto@100, Serranoham@100, Kalkoen beleg@150, Tapenade, Hummus, Zalmsalade, Eiersalade,
            Kipkerriesalade, Tonijnsalade""",
        5 to """Melk, Halfvolle melk, Volle melk, Magere melk, Lactosevrije melk, Havermelk, Sojamelk, Amandelmelk,
            Karnemelk, Chocolademelk, Yoghurt, Griekse yoghurt, Volle yoghurt, Kwark, Skyr, Vla, Vanillevla,
            Chocoladevla, Pudding, Toetjes, Slagroom, Kookroom, Crème fraîche, Zure room, Boter, Roomboter, Margarine,
            Halvarine, Eieren, Scharreleieren, Vrije-uitloopeieren, Biologische eieren, Drinkyoghurt, Kefir, Smoothie""",
        6 to """Hagelslag, Pure hagelslag, Melk hagelslag, Vlokken, Muisjes, Vruchtenhagel, Pindakaas, Jam,
            Aardbeienjam, Chocoladepasta, Nutella, Speculoospasta, Appelstroop, Honing, Sandwichspread, Muesli, Cruesli,
            Granola, Havermout@500, Cornflakes, Ontbijtgranen, Koffie, Koffiebonen, Gemalen koffie, Koffiecups,
            Koffiepads, Oploskoffie, Thee, Groene thee, Muntthee, Rooibosthee, Suiker, Zoetjes, Koffiemelk, Koffiecreamer""",
        7 to """Spaghetti, Penne, Macaroni, Fusilli, Farfalle, Tagliatelle, Lasagnebladen, Tortellini, Ravioli, Gnocchi,
            Verse pasta, Rijst, Basmatirijst, Zilvervliesrijst, Pandanrijst, Risottorijst, Sushirijst, Couscous,
            Bulgur, Quinoa, Mie, Mienestjes, Rijstnoedels, Udonnoedels, Taco's, Tacoschelpen, Burrito pakket,
            Nasi kruiden, Bami kruiden, Ketjap, Sojasaus, Sambal, Sambal oelek, Sriracha, Kokosmelk, Currypasta,
            Rode currypasta, Groene currypasta, Pindasaus, Satésaus, Kroepoek, Atjar, Babi pangang saus, Woksaus,
            Noriblaadjes, Wasabi, Pizzadeeg, Pizzabodems, Mexicaanse kruiden, Rijstpapier""",
        8 to """Tomatenpuree, Gepelde tomaten, Tomatenblokjes, Passata, Pastasaus, Pesto, Rode pesto, Bruine bonen,
            Kidneybonen, Kikkererwten, Linzen, Witte bonen, Witte bonen in tomatensaus, Mais, Doperwten en wortelen,
            Tonijn in blik, Sardientjes, Augurken, Zilveruitjes, Olijven, Groene olijven, Zwarte olijven,
            Gevulde olijven, Kappertjes, Zongedroogde tomaten, Gegrilde paprika, Appelmoes, Soep, Tomatensoep,
            Kippensoep, Erwtensoep, Groentesoep, Bouillon, Bouillonblokjes, Mayonaise, Ketchup, Mosterd, Fritessaus,
            Curry saus, Barbecuesaus, Knoflooksaus, Joppiesaus, Sladressing, Balsamico, Azijn, Jus, Hollandaisesaus,
            Champignonsaus, Rode bietjes in pot, Ananas in blik, Perziken op sap, Mandarijntjes in blik, Rode kool in pot""",
        9 to """Olijfolie, Extra vierge olijfolie, Zonnebloemolie, Arachideolie, Bakboter, Frituurolie, Zout, Zeezout,
            Peper, Zwarte peper, Paprikapoeder, Gerookte paprikapoeder, Kerriepoeder, Komijn, Kurkuma, Kaneel,
            Nootmuskaat, Oregano, Gedroogde basilicum, Tijm, Rozemarijn, Italiaanse kruiden, Chilivlokken, Cayennepeper,
            Knoflookpoeder, Laurierblaadjes, Kipkruiden, Gyroskruiden, Bloem@1000, Zelfrijzend bakmeel@500,
            Maizena, Bakpoeder, Gist, Vanillesuiker, Basterdsuiker, Poedersuiker, Rietsuiker, Cacaopoeder,
            Chocoladedruppels, Amandelmeel, Pannenkoekenmix, Cakemix, Gelatine, Rozijnen, Ahornsiroop, Stroop,
            Vanille-extract""",
        10 to """Koekjes, Biscuitjes, Stroopwafels, Speculaas, Kaneelkoekjes, Bokkenpootjes, Eierkoeken, Gevulde koeken,
            Lange vingers, Cake, Brownies, Wafels, Chocolade, Melkchocolade, Pure chocolade, Witte chocolade,
            Chocoladerepen, Bonbons, Drop, Winegums, Snoep, Kauwgom, Pepermunt, Lolly's, Marshmallows, Mueslirepen,
            Rijstwafels met chocolade, Pepernoten, Kruidnoten, Chocoladeletter, Paaseitjes, Tompoucen""",
        11 to """Chips, Paprikachips, Naturel chips, Ribbelchips, Tortillachips, Nacho's, Popcorn, Zoutjes, Pinda's,
            Borrelnootjes, Cashewnoten, Amandelen, Walnoten, Hazelnoten, Pistachenoten, Studentenhaver, Notenmix,
            Zoute stengels, Pretzels, Kaasstengels, Toastjes, Borrelhapjes, Rijstwafels, Rijstcrackers""",
        12 to """Diepvriespizza, Pizza, Patat, Friet, Ovenfriet, Aardappelkroketjes, Rösti, Bitterballen, Kroketten,
            Frikandellen, Kaassoufflés, Loempia's, Kipnuggets, Diepvriesgroente, Diepvriesspinazie, Spinazie à la crème,
            Diepvries doperwten, Diepvries wokgroente, Diepvriesfruit, Bosvruchten, IJs, IJsjes, Waterijsjes, Roomijs,
            Visfilet, Diepvries garnalen, Bladerdeeg, Croissantdeeg, Kant-en-klaarmaaltijd, Ovenschotel, Lasagne,
            IJsblokjes""",
        13 to """Water, Mineraalwater, Bruiswater, Spa rood, Spa blauw, Cola, Cola zero, Pepsi max, Fanta, Sprite, 7up,
            Ice tea, Sinas, Cassis, Tonic, Ginger ale, Sinaasappelsap, Appelsap, Multivruchtensap, Druivensap,
            Tomatensap, Ranja, Limonadesiroop, Energiedrank, Red Bull, Sportdrank, Bier, Alcoholvrij bier,
            Speciaalbier, Radler, Wijn, Rode wijn, Witte wijn, Rosé, Prosecco, Champagne, Cider, Jenever, Whisky, Rum,
            Wodka, Gin, Likeur, Port""",
        14 to """Wc-papier, Keukenrol, Tissues, Zakdoekjes, Vuilniszakken, Pedaalemmerzakken, Afwasmiddel,
            Vaatwastabletten, Vaatwaszout, Glansspoelmiddel, Wasmiddel, Wasmiddel kleur, Wasmiddel wit, Wasverzachter,
            Vlekkenverwijderaar, Allesreiniger, Schoonmaakazijn, Bleek, Wc-reiniger, Wc-blokjes, Glasreiniger,
            Badkamerreiniger, Ontkalker, Ontstopper, Schuursponsjes, Sponzen, Vaatdoekjes, Microvezeldoekjes,
            Theedoeken, Aluminiumfolie, Huishoudfolie, Bakpapier, Diepvrieszakjes, Boterhamzakjes, Batterijen,
            Lampjes, Kaarsen, Waxinelichtjes, Lucifers, Aansteker, Luchtverfrisser, Huishoudhandschoenen, Wasknijpers,
            Koffiefilters, Stofzuigerzakken, Aanmaakblokjes, Houtskool""",
        15 to """Tandpasta, Tandenborstel, Opzetborstels, Flosdraad, Mondwater, Douchegel, Zeep, Handzeep, Shampoo,
            Conditioner, Deodorant, Bodylotion, Dagcrème, Handcrème, Zonnebrand, Aftersun, Lippenbalsem, Scheermesjes,
            Scheerschuim, Wattenstaafjes, Wattenschijfjes, Maandverband, Tampons, Inlegkruisjes, Haargel, Haarlak,
            Haarelastiekjes, Make-up remover, Nagellakremover, Pleisters, Paracetamol, Ibuprofen, Keelpastilles,
            Neusspray, Vitamines, Vitamine D, Condooms, Vochtige doekjes, Muggenspray, Oordopjes, Lenzenvloeistof""",
        16 to """Luiers, Billendoekjes, Babyvoeding, Flesvoeding, Fruithapjes, Knijpfruit, Babyshampoo, Kattenvoer,
            Kattenbrokjes, Kattenbakvulling, Kattensnacks, Hondenvoer, Hondenbrokjes, Hondensnacks, Poepzakjes,
            Vogelvoer, Visvoer""",
        17 to """Bloemen, Cadeaukaart, Wenskaart, Postzegels, Tijdschrift""",
    )

    val products: List<Product> by lazy {
        val seen = HashSet<String>()
        raw.flatMap { (cat, text) ->
            text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { entry ->
                val parts = entry.split('@')
                val name = parts[0].trim()
                if (!seen.add(Suggest.norm(name))) return@mapNotNull null
                val grams = parts.getOrNull(1)?.trim()?.toLongOrNull()
                if (grams != null) Product(name, cat, "g", grams) else Product(name, cat, "st", 1)
            }
        }
    }

    private val byName: Map<String, Product> by lazy { products.associateBy { Suggest.norm(it.name) } }

    fun find(name: String): Product? = byName[Suggest.norm(name)]
}
