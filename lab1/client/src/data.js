export const TEATRU = {
    nume: "Teatrul Național de Operetă și Comedie Muzicală",
    iban: "RO00EXMP0000000000000001",
};

export const TVA = 0.21;             // inclus în prețul biletului
export const REDUCERE_STUDENT = 0.5; // 50% pentru studenți
export const MAX_BILETE = 10;

export const SPECTACOLE = {
    "Romeo și Julieta": {
        reprezentatii: [
            { data: "2026-11-05", ora: "19:00" },
            { data: "2026-11-12", ora: "19:00" },
            { data: "2026-11-25", ora: "18:30" },
        ],
        preturi: { "Sală": 140, "Lojă": 180, "Balcon": 90 },
    },
    "My Fair Lady": {
        reprezentatii: [
            { data: "2026-11-07", ora: "18:00" },
            { data: "2026-11-15", ora: "19:00" },
            { data: "2026-11-28", ora: "19:00" },
        ],
        preturi: { "Sală": 130, "Lojă": 160, "Balcon": 80 },
    },
    "Rebecca": {
        reprezentatii: [
            { data: "2026-11-10", ora: "19:00" },
            { data: "2026-11-19", ora: "19:00" },
            { data: "2026-12-03", ora: "18:30" },
        ],
        preturi: { "Sală": 150, "Lojă": 190, "Balcon": 100 },
    },
    "Chicago": {
        reprezentatii: [
            { data: "2026-11-14", ora: "19:30" },
            { data: "2026-11-21", ora: "19:30" },
            { data: "2026-12-06", ora: "18:00" },
        ],
        preturi: { "Sală": 160, "Lojă": 200, "Balcon": 110 },
    },
    "Fantoma de la Operă": {
        reprezentatii: [
            { data: "2026-11-18", ora: "19:00" },
            { data: "2026-11-26", ora: "19:00" },
            { data: "2026-12-10", ora: "19:00" },
        ],
        preturi: { "Sală": 170, "Lojă": 220, "Balcon": 120 },
    },
    "Mamma Mia!": {
        reprezentatii: [
            { data: "2026-11-20", ora: "19:00" },
            { data: "2026-11-29", ora: "18:00" },
            { data: "2026-12-13", ora: "18:00" },
        ],
        preturi: { "Sală": 140, "Lojă": 170, "Balcon": 85 },
    },
    "Les Misérables": {
        reprezentatii: [
            { data: "2026-11-22", ora: "18:00" },
            { data: "2026-12-04", ora: "19:00" },
            { data: "2026-12-15", ora: "19:00" },
        ],
        preturi: { "Stal": 155, "Lojă": 195, "Balcon": 95 },
    },
};