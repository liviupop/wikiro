package org.wikipedia.ro.populationdb.ua;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

import java.awt.Color;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jfree.data.general.DefaultPieDataset;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.wikibase.Wikibase;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ua.dao.Hibernator;
import org.wikipedia.ro.populationdb.ua.model.Commune;
import org.wikipedia.ro.populationdb.ua.model.Language;
import org.wikipedia.ro.populationdb.ua.model.LanguageStructurable;
import org.wikipedia.ro.populationdb.ua.model.Raion;
import org.wikipedia.ro.populationdb.ua.model.Region;
import org.wikipedia.ro.populationdb.util.ParameterReader;
import org.wikipedia.ro.populationdb.util.UkrainianTransliterator;
import org.wikipedia.ro.populationdb.util.Utilities;
import org.wikipedia.ro.populationdb.util.WikiEditExecutor;

public class UAWikiGenerator {

    private static final String SEP = "\n";

    public static void main(final String[] args) throws Exception {
        final UAWikiGenerator generator = new UAWikiGenerator();
        try {
            generator.init();
            generator.generateRegions();
        } finally {
            generator.close();
        }
    }

    private Wiki rowiki;
    private Wikibase dwiki;
    private WikiEditExecutor executor;
    private Hibernator hib;
    private final Map<LanguageStructurable, LazyInitializer<String>> roArticleNames = new HashMap<LanguageStructurable, LazyInitializer<String>>();
    private final Map<Language, Color> nationColorMap = new HashMap<Language, Color>();
    private final Map<String, Language> nationNameMap = new HashMap<String, Language>();
    private final Map<String, String> urls = new HashMap<String, String>() {
        {
            put("Crimeea",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_001&ti=19A050501_02_001.%20Distribution%20of%20the%20population%20by%20native%20language,%20Avtonomna%20Respublika%20Krym%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Vinița",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_005&ti=19A050501_02_005.%20Distribution%20of%20the%20population%20by%20native%20language,%20Vinnytska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Volînia",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_007&ti=19A050501_02_007.%20Distribution%20of%20the%20population%20by%20native%20language,%20Volynska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Dnipropetrovsk",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_012&ti=19A050501_02_012.%20Distribution%20of%20the%20population%20by%20native%20language,%20Dnipropetrovska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Donețk",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_014&ti=19A050501_02_014.%20Distribution%20of%20the%20population%20by%20native%20language,%20Donetska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Jîtomîr",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_018&ti=19A050501_02_018.%20Distribution%20of%20the%20population%20by%20native%20language,%20Zhytomyrska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Zaporijjea",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_023&ti=19A050501_02_023.%20Distribution%20of%20the%20population%20by%20native%20language,%20Zaporizka%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Ivano-Frankivsk",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_026&ti=19A050501_02_026.%20Distribution%20of%20the%20population%20by%20native%20language,%20Ivano-Frankivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Kiev",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_032&ti=19A050501_02_032.%20Distribution%20of%20the%20population%20by%20native%20language,%20Kyivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Kirovohrad",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_035&ti=19A050501_02_035.%20Distribution%20of%20the%20population%20by%20native%20language,%20Kirovohradska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Luhansk",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_044&ti=19A050501_02_044.%20Distribution%20of%20the%20population%20by%20native%20language,%20Luhanska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Mîkolaiiv",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_048&ti=19A050501_02_048.%20Distribution%20of%20the%20population%20by%20native%20language,%20Mykolaivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Odesa",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_051&ti=19A050501_02_051.%20Distribution%20of%20the%20population%20by%20native%20language,%20Odeska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Poltava",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_053&ti=19A050501_02_053.%20Distribution%20of%20the%20population%20by%20native%20language,%20Poltavska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Rivne",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_056&ti=19A050501_02_056.%20Distribution%20of%20the%20population%20by%20native%20language,%20Rivnenska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Sumî",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_059&ti=19A050501_02_059.%20Distribution%20of%20the%20population%20by%20native%20language,%20Sumska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Ternopil",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_061&ti=19A050501_02_061.%20Distribution%20of%20the%20population%20by%20native%20language,%20Ternopilska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Harkiv",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_063&ti=19A050501_02_063.%20Distribution%20of%20the%20population%20by%20native%20language,%20Kharkivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Herson",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_065&ti=19A050501_02_065.%20Distribution%20of%20the%20population%20by%20native%20language,%20Khersonska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Hmelnîțkîi",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_068&ti=19A050501_02_068.%20Distribution%20of%20the%20population%20by%20native%20language,%20Khmelnytska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Cerkasî",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_071&ti=19A050501_02_071.%20Distribution%20of%20the%20population%20by%20native%20language,%20Cherkaska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Cernăuți",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_073&ti=19A050501_02_073.%20Distribution%20of%20the%20population%20by%20native%20language,%20Chernivetska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Cernigău",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_074&ti=19A050501_02_074.%20Distribution%20of%20the%20population%20by%20native%20language,%20Chernihivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Sevastopol",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_085&ti=19A050501_02_085.%20Distribution%20of%20the%20population%20by%20native%20language,%20Sevastopol%20(miskrada)%20(1,2,3,4)&path=../Database/Census/05/01/&lang=2&multilang=en");
            put("Transcarpatia",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_021&ti=19A050501_02_021.%20Distribution%20of%20the%20population%20by%20native%20language,%20Zakarpatska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/01/&lang=2&multilang=en");
            put("Liov",
                "http://database.ukrcensus.gov.ua/MULT/Dialog/varval.asp?ma=19A050501_02_046&ti=19A050501_02_046.%20Distribution%20of%20the%20population%20by%20native%20language,%20Lvivska%20oblast%20(1,2,3,4)&path=../Database/Census/05/01/02/&lang=2&multilang=en");
        }
    };
    private Wiki ukwiki;

    private void generateRegions() throws IOException {
        hib.getSession().beginTransaction();
        final List<Region> regions = hib.getAllRegions();
        for (final Region eachReg : regions) {
            generateRegionText(eachReg);
            generateRegionNavBox(eachReg);
        }
    }

    private void generateRegionNavBox(final Region region) {
        final List<Commune> regionalCities = hib.getRegionalCitiesForRegion(region);
        final List<Raion> raions = hib.getRaionsForRegion(region);
        int section = 0;

        final StringBuilder navBox = new StringBuilder(
            "{{Casetă de navigare simplă\n|stare = {{{stare|autopliabilă}}}\n|titlu=Diviziuni administrative ale [[Regiunea");
        final String regionRomanianName = obtainActualRomanianName(region);
        navBox.append(regionRomanianName);
        navBox.append('|');
        navBox.append(regionRomanianName);
        navBox.append("]]\n|nume=Regiunea " + regionRomanianName);

        if (null != regionalCities && 0 < regionalCities.size()) {
            section++;
            navBox.append("\n|grup").append(section).append("=Orașe regionale");
            navBox.append("\n|listă").append(section).append("=<div>");
            navBox.append("</div>");
        }
        if (null != raions && 0 < raions.size()) {
            section++;
            navBox.append("\n|grup").append(section).append("=[[Raioanele Ucrainei|Raioane]]");
            navBox.append("\n|listă").append(section).append("=<div>");
            navBox.append("</div>");
        }
        navBox.append("}}<noinclude>");
        navBox.append("[[Categorie:Formate de navigare regiuni din Ucraina|");
        navBox.append(regionRomanianName);
        navBox.append("]]");
        navBox.append("</noinclude>");
    }

    private void generateRegionText(final Region region) throws IOException {
        if (equalsIgnoreCase("orașul Kiev", region.getRomanianName())) {
            return;
        }

        final List<String> candidateRegionArticleNames = new ArrayList<String>();
        if (isNotBlank(region.getRomanianName())) {
            candidateRegionArticleNames.add("Regiunea " + region.getRomanianName());
            candidateRegionArticleNames.add("Regiunea " + region.getRomanianName() + ", Ucraina");
        }
        candidateRegionArticleNames.add("Regiunea " + region.getTransliteratedName());
        candidateRegionArticleNames.add("Regiunea " + region.getTransliteratedName() + ", Ucraina");

        if (equalsIgnoreCase(region.getRomanianName(), "Crimeea")) {
            for (int i = 0; i < candidateRegionArticleNames.size(); i++) {
                candidateRegionArticleNames.set(i,
                    replace(candidateRegionArticleNames.get(i), "Regiunea", "Republica Autonomă"));
            }
        }

        String actualTitle = null;
        final StringBuilder currentText = new StringBuilder();
        final boolean[] titleExistance = rowiki.exists(candidateRegionArticleNames
            .toArray(new String[candidateRegionArticleNames.size()]));
        for (int i = 0; i < titleExistance.length; i++) {
            if (!titleExistance[i]) {
                continue;
            }
            final String eachCandidateTitle = candidateRegionArticleNames.get(i);
            actualTitle = defaultString(rowiki.resolveRedirect(eachCandidateTitle), eachCandidateTitle);
        }
        if (null == actualTitle) {
            actualTitle = candidateRegionArticleNames.get(0);
        } else {
            currentText.append(rowiki.getPageText(actualTitle));
        }
        final ParameterReader ibParaReader = new ParameterReader(currentText.toString());
        ibParaReader.run();
        int templateLength = ibParaReader.getTemplateLength();
        if (!equalsIgnoreCase(region.getRomanianName(), "Crimeea")) {
            final String regionInfobox = generateRegionInfobox(region, ibParaReader);
            String regionIntro = generateRegionIntro(region, actualTitle);

            String currentRegionIntro = substringBefore(substring(currentText.toString(), templateLength), "==");
            currentRegionIntro = substringBefore(currentRegionIntro, "{{Ucraina}}");
            currentRegionIntro = trim(currentRegionIntro);
            if (currentRegionIntro.length() < regionIntro.length()) {
                regionIntro = currentRegionIntro;
            }

            currentText.replace(0, ibParaReader.getTemplateLength(), regionInfobox);
            templateLength = regionInfobox.length();
            int indexOfFirstSection = currentText.indexOf("==", ibParaReader.getTemplateLength());
            if (0 > indexOfFirstSection) {
                indexOfFirstSection = currentText.indexOf("{{Ucraina}}");
            }
            currentText.replace(templateLength, indexOfFirstSection, SEP + regionIntro + SEP);

            final String demografie = generateDemographySection(region);

            final int indexOfCurrentDemography = locateFirstOf(currentText, "==Populați", "== Populați", "== Demografie",
                "==Demografie");
            if (0 <= indexOfCurrentDemography) {
                currentText.replace(indexOfCurrentDemography, currentText.indexOf("==", indexOfCurrentDemography + 2) + 2,
                    demografie);
            } else {
                final int indexOfFutureDemographySection = locateFirstOf(currentText, "== Economie", "==Economie",
                    "{{Ucraina", "==Legături externe", "== Legături externe", "== Vezi și", "==Vezi și");
                if (0 <= indexOfFutureDemographySection) {
                    currentText.insert(indexOfFutureDemographySection, demografie);
                } else {
                    currentText.append(demografie);
                }
            }
            System.out.println(currentText);
        }
    }

    private int locateFirstOf(final CharSequence haystack, final CharSequence... needles) {
        final List<Integer> locations = new ArrayList<Integer>();
        for (final CharSequence eachNeedle : needles) {
            final int locationOfNeedleInHaystack = indexOf(haystack, eachNeedle);
            if (0 <= locationOfNeedleInHaystack) {
                locations.add(locationOfNeedleInHaystack);
            }
        }
        if (0 == locations.size()) {
            return -1;
        }
        Collections.sort(locations);
        return locations.get(0);
    }

    private String generateDemographySection(final LanguageStructurable place) {
        final StringBuilder sb = new StringBuilder("== Demografie ==");
        sb.append(SEP);
        sb.append("<!-- Start secțiune generată de Andrebot -->");
        final STGroup templateGroup = new STGroupFile("templates/ua/ucraina.stg");
        final ST piechart = templateGroup.getInstanceOf("piechart");
        piechart.add("nume", defaultIfBlank(place.getRomanianName(), place.getTransliteratedName()));
        piechart.add("tip_genitiv", place.getGenitive());

        final DefaultPieDataset datasetLang = new DefaultPieDataset();
        computeEthnicityDataset(place.getLanguageStructure(), datasetLang);
        renderPiechart(sb, piechart, datasetLang);

        String templateName = "NoMaj";
        Language majLanguage = null;
        for (final Entry<Language, Double> eachEntry : place.getLanguageStructure().entrySet()) {
            final Double val = eachEntry.getValue();
            if (null == val) {
                continue;
            }
            if (val.doubleValue() >= 100.0) {
                templateName = "Total";
                majLanguage = eachEntry.getKey();
                break;
            }
            if (val.doubleValue() >= 50.0) {
                majLanguage = eachEntry.getKey();
                templateName = "Maj";
                break;
            }
        }
        final ST demoText = templateGroup.getInstanceOf("demography" + templateName);
        final NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(2);
        demoText.add("nume_unitate_genitiv", place.getGenitive() + " " + obtainActualRomanianName(place));
        if (null != majLanguage) {
            demoText.add("nume_limba_majoritara", linkifyLanguage(majLanguage));
        }
        if (StringUtils.equals("Maj", templateName)) {
            demoText.add("procent_limba_majoritara",
                "{{formatnum:" + nf.format(place.getLanguageStructure().get(majLanguage)) + "}}");
        }
        if (!StringUtils.equals("Total", templateName)) {
            final List<Language> otherLanguages = new ArrayList<Language>();
            for (final Language eachLang : place.getLanguageStructure().keySet()) {
                if (!eachLang.equals(majLanguage) && place.getLanguageStructure().get(eachLang) > 1.0) {
                    otherLanguages.add(eachLang);
                }
            }
            Collections.sort(otherLanguages, new Comparator<Language>() {

                public int compare(final Language arg0, final Language arg1) {
                    final double pop0 = defaultIfNull(place.getLanguageStructure().get(arg0), 0.0);
                    final double pop1 = defaultIfNull(place.getLanguageStructure().get(arg1), 0.0);
                    return (int) Math.signum(pop1 - pop0);
                }
            });
            final List<String> languageEnumerationList = new ArrayList<String>();
            for (final Language eachOtherLang : otherLanguages) {
                Double speakers;
                if (null != (speakers = place.getLanguageStructure().get(eachOtherLang))) {
                    final StringBuilder langBuilder = new StringBuilder(linkifyLanguage(eachOtherLang));
                    langBuilder.append(" (");
                    langBuilder.append("{{formatnum:");
                    langBuilder.append(nf.format(speakers.doubleValue()));
                    langBuilder.append("}}%)");
                    languageEnumerationList.add(langBuilder.toString());
                }
            }
            if (1 == languageEnumerationList.size()) {
                demoText.add("enum_alte_limbi", languageEnumerationList.get(0));
            } else {
                final String[] languageEnumerationArray = languageEnumerationList.toArray(new String[languageEnumerationList
                    .size()]);
                demoText.add("enum_alte_limbi",
                    join(ArrayUtils.subarray(languageEnumerationArray, 0, languageEnumerationArray.length - 1), ", ")
                        + " și " + languageEnumerationArray[languageEnumerationArray.length - 1]);
            }
        }
        final StringBuilder refBuilder = new StringBuilder("<ref name=\"populatie_ucraina_2001\">{{Citat web|url=");
        final String regionName = obtainActualRomanianName(place.computeRegion());
        refBuilder.append(urls.get(regionName));
        refBuilder.append("|publisher=Institutul Național de Statistică al Ucrainei");
        refBuilder.append("|title=Rezultatele recensământului din 2001 cu structura lingvistică a regiunii");
        refBuilder.append(regionName);
        refBuilder.append(" pe localități");
        refBuilder.append("|accessdate=2014-08-25}}");
        refBuilder.append("</ref>");
        demoText.add("ref", refBuilder.toString());
        sb.append(demoText.render());
        sb.append(SEP);

        return sb.toString();
    }

    private String obtainActualRomanianName(final LanguageStructurable place) {
        return defaultIfBlank(place.getRomanianName(), place.getTransliteratedName());
    }

    private String linkifyLanguage(final Language lang) {
        final StringBuilder linkBuilder = new StringBuilder("[[Limba ");
        final String langName = lowerCase(lang.getName());
        linkBuilder.append(langName);
        linkBuilder.append('|');
        linkBuilder.append(langName).append("]]");

        return linkBuilder.toString();
    }

    private void computeEthnicityDataset(final Map<Language, Double> languageStructure, final DefaultPieDataset dataset) {
        final Set<Language> ethnicitiesSet = nationColorMap.keySet();
        final List<Language> ethnicitiesList = new ArrayList<Language>(ethnicitiesSet);
        Collections.sort(ethnicitiesList, new Comparator<Language>() {

            public int compare(final Language arg0, final Language arg1) {
                final double natpop0 = defaultIfNull(languageStructure.get(arg0), 0.0);
                final double natpop1 = defaultIfNull(languageStructure.get(arg1), 0.0);
                return (int) Math.signum(natpop1 - natpop0);
            }

        });
        double others = 0.0;
        for (final Language nat : ethnicitiesList) {
            final Double natpop = languageStructure.get(nat);
            if (null != natpop && 1.0 < natpop.doubleValue()) {
                dataset.setValue(nat.getName(), natpop.doubleValue());
            } else if (null != natpop) {
                others += natpop;
            }
        }
        if (0.0 < others) {
            dataset.setValue("Alte limbi", others);
        }
    }

    private void renderPiechart(final StringBuilder demographics, final ST piechart, final DefaultPieDataset dataset) {
        final StringBuilder pieChartLangProps = new StringBuilder();
        int i = 1;
        demographics.append("<div style=\"float:left\">");

        final NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(2);

        for (final Object k : dataset.getKeys()) {
            pieChartLangProps.append("\n|label");
            pieChartLangProps.append(i);
            pieChartLangProps.append('=');
            pieChartLangProps.append(k.toString());
            pieChartLangProps.append("|value");
            pieChartLangProps.append(i);
            pieChartLangProps.append('=');
            pieChartLangProps.append(nf.format(dataset.getValue(k.toString()).doubleValue()));
            pieChartLangProps.append("|color");
            pieChartLangProps.append(i);
            pieChartLangProps.append('=');
            final Color color = nationColorMap.get(nationNameMap.get(k.toString()));
            if (null == color) {
                throw new RuntimeException("Unknown color for nationality " + k);
            }
            pieChartLangProps.append(Utilities.colorToHtml(color));
            i++;
        }
        piechart.add("props", pieChartLangProps.toString());
        demographics.append(piechart.render());
        demographics.append("</div>");
        demographics.append(SEP);
    }

    private String generateRegionIntro(final Region region, final String articleName) throws IOException {
        final STGroup stgroup = new STGroupFile("templates/ua/ucraina.stg");
        final ST introTmpl = stgroup.getInstanceOf("introReg");
        introTmpl.add("nume_reg_ro", defaultIfBlank(region.getRomanianName(), region.getTransliteratedName()));
        final String ukArticleName = getUkrainianRegionName(articleName);
        final String transliteratedUkArticleName = replace(new UkrainianTransliterator(ukArticleName).transliterate(),
            "Oblast", "oblast");

        introTmpl.add("nume_reg_uk", ukArticleName + "|" + transliteratedUkArticleName);

        introTmpl.add("nume_capitala", null == region.getCapital() ? "" : obtainActualRomanianName(region.getCapital()));
        return introTmpl.render();
    }

    private String getUkrainianRegionName(final String articleName) throws IOException {
        final String ukTitle = dwiki.getTitleInLanguage("rowiki", articleName, "uk");
        return ukTitle;
    }

    private String generateRegionInfobox(final Region region, final ParameterReader ibParaReader) {
        final StringBuilder sb = new StringBuilder("{{Infocaseta Regiune");
        sb.append("\n|nume = ");
        sb.append(defaultIfBlank(region.getRomanianName(), region.getTransliteratedName()));
        if (null != region.getCapital()) {
            final Commune capital = region.getCapital();
            sb.append("\n|capitala = [[");
            sb.append(defaultIfBlank(capital.getRomanianName(), capital.getTransliteratedName()));
            sb.append("]]");
        }
        sb.append("}}");
        return sb.toString();
    }

    private void init() throws FailedLoginException, IOException {
        rowiki = new Wiki("ro.wikipedia.org");
        ukwiki = new Wiki("uk.wikipedia.org");
        dwiki = new Wikibase();
        executor = new WikiEditExecutor(rowiki, dwiki);
        // executor = new SysoutExecutor();

        final Properties credentials = new Properties();
        credentials.load(UAWikiGenerator.class.getClassLoader().getResourceAsStream("credentials.properties"));
        final String datauser = credentials.getProperty("UsernameData");
        final String datapass = credentials.getProperty("PasswordData");
        final String user = credentials.getProperty("Username");
        final String pass = credentials.getProperty("Password");
        rowiki.login(user, pass.toCharArray());
        rowiki.setMarkBot(true);
        dwiki.login(datauser, datapass.toCharArray());

        hib = new Hibernator();
        final Session ses = hib.getSession();
        ses.beginTransaction();

        assignColorToLanguage("Română", new Color(85, 85, 255));
        assignColorToLanguage("Romani", new Color(85, 255, 255));
        assignColorToLanguage("Greacă", new Color(0, 0, 192));
        assignColorToLanguage("Maghiară", new Color(85, 255, 85));
        assignColorToLanguage("Belarusă", new Color(32, 192, 32));
        assignColorToLanguage("Bulgară", new Color(0, 192, 0));
        assignColorToLanguage("Tătară crimeeană", new Color(192, 192, 255));
        assignColorToLanguage("Ebraică", new Color(192, 255, 255));
        assignColorToLanguage("Karaim", new Color(32, 192, 192));
        assignColorToLanguage("Germană", new Color(255, 85, 255));
        assignColorToLanguage("Ucraineană", new Color(255, 255, 85));
        assignColorToLanguage("Rusă", new Color(192, 85, 85));
        assignColorToLanguage("Slovacă", new Color(48, 48, 160));
        assignColorToLanguage("Alte limbi", new Color(85, 85, 85));
        ses.getTransaction().rollback();
        blandifyColors(nationColorMap);
    }

    private void assignColorToLanguage(final String languageName, final Color color) throws HibernateException {
        final Language nat = hib.getLanguageByName(languageName);
        if (null != nat) {
            nationColorMap.put(nat, color);
            nationNameMap.put(nat.getName(), nat);
        }
    }

    private void close() {
        Session ses;
        if (null != (ses = hib.getSession())) {
            final org.hibernate.Transaction tx = ses.getTransaction();
            if (null != tx) {
                tx.rollback();
            }
        }
        if (null != rowiki) {
            rowiki.logout();
        }
        if (null != ukwiki) {
            ukwiki.logout();
        }
        if (null != dwiki) {
            dwiki.logout();
        }

    }

    private static <T extends Object> void blandifyColors(final Map<T, Color> colorMap) {
        for (final T key : colorMap.keySet()) {
            final Color color = colorMap.get(key);
            final int[] colorcomps = new int[3];
            colorcomps[0] = color.getRed();
            colorcomps[1] = color.getGreen();
            colorcomps[2] = color.getBlue();

            for (int i = 0; i < colorcomps.length; i++) {
                if (colorcomps[i] == 0) {
                    colorcomps[i] = 0x3f;
                } else if (colorcomps[i] == 85) {
                    colorcomps[i] = 128;
                } else if (colorcomps[i] == 64) {
                    colorcomps[i] = 85;
                } else if (colorcomps[i] == 128) {
                    colorcomps[i] = 0x9f;
                }
            }
            colorMap.put(key, new Color(colorcomps[0], colorcomps[1], colorcomps[2]));
        }
    }
}
