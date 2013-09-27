package info.astroe.populationdb.bg;

import static info.astroe.populationdb.util.Utilities.capitalizeName;
import static info.astroe.populationdb.util.Utilities.transliterateBg;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.trim;
import info.astroe.populationdb.bg.model.Obshtina;
import info.astroe.populationdb.bg.model.Region;
import info.astroe.populationdb.bg.model.Settlement;
import info.astroe.populationdb.util.HibernateUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class BGPopulationParser {
    private static SessionFactory sessionFactory = initHibernate();

    public static void main(final String[] args) {
        // TODO Auto-generated method stub
        if (2 > args.length) {
            System.out.println("Please specify two xlsx files (age, ethnos) to read populace from.");
            System.exit(1);
        }
        final File infileAge = new File(args[0]);
        if (!infileAge.exists() || !infileAge.isFile()) {
            System.out.println("Please specify a proper age xlsx file to read populace from.");
            System.exit(1);
        }
        final File infileEthnos = new File(args[1]);
        if (!infileAge.exists() || !infileAge.isFile()) {
            System.out.println("Please specify a proper ethnos xlsx file to read populace from.");
            System.exit(1);
        }

        try {
            // parseAgeFile(infileAge);
            parseEthnosFile(infileEthnos);
        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            sessionFactory.close();
        }

    }

    private static void parseEthnosFile(final File infileEthnos) throws FileNotFoundException, IOException {
        FileInputStream infileStream = null;
        try {
            final HSSFWorkbook wb = new HSSFWorkbook(infileStream = new FileInputStream(infileEthnos));

            final HSSFSheet sheet = wb.getSheetAt(0);

            final Iterator<Row> rowIterator = sheet.iterator();

            Region currentRegion = null;
            Obshtina currentObshtina = null;

            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                final Iterator<Cell> cellIterator = row.cellIterator();
                if (!cellIterator.hasNext()) {
                    continue;
                }

                final Cell nameCell = cellIterator.next();
                if (nameCell.getCellType() != Cell.CELL_TYPE_STRING) {
                    continue;
                }
                final String name = nameCell.getStringCellValue();
                if (name.length() < 2) {
                    continue;
                }

                // read population; if this isn't a number, then skip
                if (!cellIterator.hasNext()) {
                    continue;
                }
                final Cell popCell = cellIterator.next();
                if (popCell.getCellType() != Cell.CELL_TYPE_NUMERIC) {
                    continue;
                }
                final int population = (int) popCell.getNumericCellValue();
                if (population > 6e6) {
                    continue;
                }
                final CellStyle cellStyle = nameCell.getCellStyle();
                final HSSFFont usedFont = wb.getFontAt(cellStyle.getFontIndex());
                final Session ses = sessionFactory.getCurrentSession();
                ses.beginTransaction();
                if (usedFont.getBoldweight() > 400) {
                    System.out.println("Region " + capitalizeName(transliterateBg(trim(name))));
                    final Query regionQuery = ses.createQuery("from Region as region where region.numeBg=:name");
                    regionQuery.setParameter("name", capitalizeName(trim(name)));
                    final List<Region> regions = regionQuery.list();
                    if (0 == regions.size()) {
                        System.err.println("REGION OBJECT NOT FOUND FOR REGION " + capitalizeName(trim(name)));
                    } else {
                        currentRegion = regions.get(0);
                    }
                } else if (usedFont.getItalic()) {
                    System.out.println("  Obshtina " + capitalizeName(transliterateBg(name)));
                    final Query obshtinaQuery = ses
                        .createQuery("from Obshtina as obs where obs.numeBg=:name and obs.region=:region");
                    obshtinaQuery.setParameter("name", capitalizeName(trim(name)));
                    obshtinaQuery.setParameter("region", currentRegion);
                    final List<Obshtina> obshtinas = obshtinaQuery.list();
                    if (0 == obshtinas.size()) {
                        System.err.println("OBSHTINA OBJECT NOT FOUND FOR REGION " + capitalizeName(trim(name)));
                    } else {
                        currentObshtina = obshtinas.get(0);
                    }

                } else {
                    final String settlementName = substringAfter(name, ".");
                    final Query settlementQuery = ses
                        .createQuery("from Settlement as settlement where settlement.numeBg=:name and settlement.obshtina=:obshtina and settlement.obshtina.region=:region");
                    settlementQuery.setParameter("name", capitalizeName(trim(settlementName)));
                    settlementQuery.setParameter("obshtina", currentObshtina);
                    settlementQuery.setParameter("region", currentRegion);
                    final List<Settlement> settlements = settlementQuery.list();
                    Settlement village = null;
                    if (0 == settlements.size()) {
                        System.err.println("SETTLEMENT OBJECT NOT FOUND FOR REGION " + capitalizeName(trim(settlementName)));
                    } else {
                        village = settlements.get(0);
                    }
                    if (startsWith(name, "ГР")) {
                        System.out.println("    Town " + capitalizeName(transliterateBg(settlementName)));
                    } else {
                        System.out.println("    Village " + capitalizeName(transliterateBg(settlementName)));
                    }
                }
                ses.getTransaction().commit();
            }
        } finally {
            IOUtils.closeQuietly(infileStream);
        }
    }

    private static void parseAgeFile(final File infileAge) throws FileNotFoundException, IOException {
        // final SessionFactory hibernateSf = initHibernate();
        FileInputStream infileStream = null;
        final HSSFWorkbook wb = new HSSFWorkbook(infileStream = new FileInputStream(infileAge));

        final HSSFSheet sheet = wb.getSheetAt(0);

        final Iterator<Row> rowIterator = sheet.iterator();

        Region currentRegion = null;
        Obshtina currentObshtina = null;

        while (rowIterator.hasNext()) {
            final Row row = rowIterator.next();
            final Iterator<Cell> cellIterator = row.cellIterator();
            if (!cellIterator.hasNext()) {
                continue;
            }

            final Cell nameCell = cellIterator.next();
            if (nameCell.getCellType() != Cell.CELL_TYPE_STRING) {
                continue;
            }
            final String name = nameCell.getStringCellValue();
            if (name.length() < 2) {
                continue;
            }

            // read population; if this isn't a number, then skip
            if (!cellIterator.hasNext()) {
                continue;
            }
            final Cell popCell = cellIterator.next();
            if (popCell.getCellType() != Cell.CELL_TYPE_NUMERIC) {
                continue;
            }
            final int population = (int) popCell.getNumericCellValue();
            if (population > 7e6) {
                continue;
            }
            final CellStyle cellStyle = nameCell.getCellStyle();
            final int indent = cellStyle.getIndention();
            final Session ses = sessionFactory.getCurrentSession();
            ses.beginTransaction();
            if (0 == indent) {
                currentRegion = new Region();
                currentRegion.setNumeBg(trim(capitalizeName(name)));
                currentRegion.setNumeRo(capitalizeName(transliterateBg(trim(name))));
                System.out.println("Region " + currentRegion.getNumeBg() + "/" + currentRegion.getNumeRo() + " pop "
                    + population);
                ses.save(currentRegion);
            } else if (1 == indent) {
                currentObshtina = new Obshtina();
                currentObshtina.setNumeBg(capitalizeName(trim(name)));
                currentObshtina.setNumeRo(capitalizeName(transliterateBg(trim(name))));
                currentObshtina.setPopulation(population);
                currentObshtina.setRegion(currentRegion);
                currentRegion.getObshtinas().add(currentObshtina);
                ses.save(currentObshtina);
            } else {
                final Settlement village = new Settlement();
                village.setTown(startsWith(name, "ГР"));
                final String villageName = substringAfter(name, ".");
                village.setNumeBg(capitalizeName(trim(villageName)));
                village.setNumeRo(capitalizeName(transliterateBg(trim(villageName))));
                village.setPopulation(population);
                village.setObshtina(currentObshtina);
                currentObshtina.getSettlements().add(village);
                ses.save(village);
            }
            ses.getTransaction().commit();
        }
    }

    private static SessionFactory initHibernate() {
        return HibernateUtil.getSessionFactory(null);
    }

}
