package org.wikipedia.ro.monuments.monuments_section.generators;

import static org.wikipedia.ro.monuments.monuments_section.Utils.joinWithConjunction;

import java.util.List;

import org.wikipedia.ro.monuments.monuments_section.NumberToWordsConvertor;
import org.wikipedia.ro.monuments.monuments_section.data.Monument;

public class LocalMonumentsListGenerator extends AbstractMonumentGenerator {
    public String generate(List<Monument> monList) {
        List<List<Monument>> splitMonuments = splitMonumentsByType(monList);
        if (splitMonuments.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        if (splitMonuments.size() == 1) { // only one type of monuments
            if (splitMonuments.get(0).size() == 1) { // only one monument total
                Monument theMonument = splitMonuments.get(0).get(0);
                sb.append(": ");
                sb.append(MONUMENT_TYPE_DESCRIPTIONS[theMonument.type][1]).append(" de interes local ");
                sb.append(theMonument.name);
                sb.append(" datând din ").append(theMonument.dating);
                if (0 < theMonument.submonuments.size()) {
                    sb.append(", ansamblu alcătuit din ");
                    sb.append(retrieveSubmonumentsText(theMonument));
                }
            } else { // more monuments of only one type
                sb.append(", toate clasificate ca ").append(MONUMENT_TYPE_DESCRIPTIONS[splitMonuments.get(0).get(0).type][2])
                    .append(" de interes local: ");
                List<String> monumentDescriptions = generateMonumentsListDescription(splitMonuments.get(0));
                sb.append(joinWithConjunction(monumentDescriptions, ", ", " și "));
            }
        } else { // more types of monuments
            String introWordSingle = "Unul";
            String introWordMultiple = "";
            sb.append(new NumberToWordsConvertor(monList.size()).convert()).append(' ')
                .append(MONUMENT_TYPE_DESCRIPTIONS[0][2]).append(" de interes local ");
            for (List<Monument> eachMonumentTypeList : splitMonuments) {
                if (1 == eachMonumentTypeList.size()) {
                    sb.append(introWordSingle).append(" este ")
                        .append(MONUMENT_TYPE_DESCRIPTIONS[eachMonumentTypeList.get(0).type][1]).append(' ');

                } else {
                    sb.append(introWordMultiple).append(' ')
                        .append(new NumberToWordsConvertor(eachMonumentTypeList.size()).convert()).append(" sunt ")
                        .append(MONUMENT_TYPE_DESCRIPTIONS[eachMonumentTypeList.get(0).type][3]).append(": ");
                }
                introWordSingle = "Altul";
                introWordMultiple = "Alte";
                List<String> monumentDescriptions = generateMonumentsListDescription(eachMonumentTypeList);
                sb.append(joinWithConjunction(monumentDescriptions, "; ", "; și ")).append(". ");
            }
        }
        return sb.toString();
    }
}
