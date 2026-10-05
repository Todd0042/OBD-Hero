package com.obdhero.app.domain

object VinDecoder {

    private val YEAR_CODES = mapOf(
        'A' to 2010, 'B' to 2011, 'C' to 2012, 'D' to 2013, 'E' to 2014,
        'F' to 2015, 'G' to 2016, 'H' to 2017, 'J' to 2018, 'K' to 2019,
        'L' to 2020, 'M' to 2021, 'N' to 2022, 'P' to 2023, 'R' to 2024,
        'S' to 2025, 'T' to 2026, 'V' to 2027, 'W' to 2028, 'X' to 2029,
        'Y' to 2030, '1' to 2001, '2' to 2002, '3' to 2003, '4' to 2004,
        '5' to 2005, '6' to 2006, '7' to 2007, '8' to 2008, '9' to 2009
    )

    private val WMI_TO_MAKE = mapOf(
        "1FA" to "Ford", "1FB" to "Ford", "1FC" to "Ford", "1FD" to "Ford", "1FM" to "Ford", "1FT" to "Ford",
        "2FA" to "Ford", "2FB" to "Ford", "2FM" to "Ford", "2FT" to "Ford", "3FA" to "Ford", "3FT" to "Ford",
        "1G1" to "Chevrolet", "1GC" to "Chevrolet", "2G1" to "Chevrolet", "3GC" to "Chevrolet",
        "1GT" to "GMC", "2GT" to "GMC", "3GT" to "GMC",
        "1G6" to "Cadillac", "1G4" to "Buick",
        "4T1" to "Toyota", "4T4" to "Toyota", "5TB" to "Toyota", "2T1" to "Toyota", "JT2" to "Toyota", "JTE" to "Toyota",
        "JT3" to "Toyota", "JTD" to "Toyota", "JTN" to "Toyota",
        "1HG" to "Honda", "2HG" to "Honda", "3HG" to "Honda", "5J6" to "Honda", "JHM" to "Honda",
        "1N4" to "Nissan", "3N1" to "Nissan", "5N1" to "Nissan", "JN1" to "Nissan",
        "KM8" to "Hyundai", "KMH" to "Hyundai", "5NP" to "Hyundai",
        "KNA" to "Kia", "KND" to "Kia", "5XX" to "Kia",
        "4S3" to "Subaru", "4S4" to "Subaru", "JF1" to "Subaru", "JF2" to "Subaru",
        "JM1" to "Mazda", "JM3" to "Mazda", "3MZ" to "Mazda",
        "1C3" to "Chrysler", "1C4" to "Chrysler/Jeep", "1C6" to "Ram", "2C3" to "Dodge", "3C6" to "Ram",
        "WBA" to "BMW", "WBS" to "BMW M", "WMW" to "Mini", "5UX" to "BMW", "4US" to "BMW",
        "WVW" to "Volkswagen", "3VW" to "Volkswagen", "1V2" to "Volkswagen",
        "WAU" to "Audi", "WA1" to "Audi"
    )

    fun decode(rawVin: String): VehicleProfile {
        val cleanVin = rawVin.trim().uppercase().replace("[^A-Z0-9]".toRegex(), "")
        if (cleanVin.length < 17) {
            return VehicleProfile(vin = cleanVin, year = null, make = "Unknown", model = "Vehicle")
        }

        val wmi = cleanVin.substring(0, 3)
        val make = WMI_TO_MAKE[wmi] ?: when (cleanVin.first()) {
            '1', '4', '5' -> "US Domestic"
            '2' -> "Canada Origin"
            '3' -> "Mexico Origin"
            'J' -> "Japanese Origin"
            'K' -> "Korean Origin"
            'W' -> "German Origin"
            else -> "Standard Vehicle"
        }

        val yearChar = cleanVin[9]
        val year = YEAR_CODES[yearChar]

        // Check for BMW / Mini E-Series / R-Series FRM vulnerability
        val isBmwOrMini = make.contains("BMW", ignoreCase = true) || make.contains("Mini", ignoreCase = true)
        val isBmwFrmVulnerable = isBmwOrMini && (year != null && year in 2006..2016)

        // Check for Stellantis / GM Security Gateway (SGW)
        val isStellantis = make.contains("Chrysler", ignoreCase = true) ||
                make.contains("Jeep", ignoreCase = true) ||
                make.contains("Dodge", ignoreCase = true) ||
                make.contains("Ram", ignoreCase = true)
        val isSgwLocked = (isStellantis && year != null && year >= 2018) ||
                (make.contains("Chevrolet", ignoreCase = true) && year != null && year >= 2021)

        val modelGuess = when (make) {
            "BMW" -> if (cleanVin[3] == '3') "3-Series" else if (cleanVin[3] == '5') "5-Series" else if (cleanVin[3] == 'X') "X-Series" else "Sedan/SAV"
            "Ford" -> if (cleanVin.contains("F15") || cleanVin.startsWith("1FT")) "F-150" else if (cleanVin.contains("MU")) "Mustang" else "Vehicle"
            "Chevrolet" -> if (cleanVin.startsWith("1GC")) "Silverado" else "Passenger Car"
            "Toyota" -> if (cleanVin.startsWith("4T1")) "Camry" else if (cleanVin.startsWith("JTE")) "RAV4" else "Vehicle"
            "Mazda" -> if (cleanVin.startsWith("JM1ND")) "MX-5 Miata" else "Mazda Vehicle"
            else -> "Standard Model"
        }

        return VehicleProfile(
            vin = cleanVin,
            year = year,
            make = make,
            model = modelGuess,
            isBmwFrmVulnerable = isBmwFrmVulnerable,
            isSgwLocked = isSgwLocked
        )
    }
}
