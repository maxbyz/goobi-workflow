package de.sub.goobi.persistence.managers;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi-workflow
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.goobi.beans.HistoryEvent;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class HistoryManager implements Serializable {
    private static final long serialVersionUID = -6083287984702600461L;

    public static List<HistoryEvent> getHistoryEvents(int id) {

        try {
            return HistoryMysqlHelper.getHistoryEvents(id);
        } catch (SQLException e) {
            log.error(e);
        }
        return new ArrayList<HistoryEvent>();
    }

    public static void addHistory(Date myDate, double order, String value, int type, int processId) {
        try {
            HistoryMysqlHelper.addHistory(myDate, order, value, type, processId);
        } catch (SQLException e) {
            log.error("Cannot not save history event", e);
        }
    }

    public static void addHistoryEvent(HistoryEvent he) {
        addHistory(he.getDate(), he.getNumericValue(), he.getStringValue(), he.getHistoryType().getValue(), he.getProcess().getId());
    }

    public static void updateHistoryEvent(HistoryEvent he) {
        try {
            HistoryMysqlHelper.updateHistoryEvent(he);
        } catch (SQLException e) {
            log.error("Cannot not save history event", e);
        }
    }

    public static void deleteHistoryEvent(HistoryEvent he) {
        try {
            HistoryMysqlHelper.deleteHistoryEvent(he);
        } catch (SQLException e) {
            log.error("Cannot not save history event", e);
        }
    }

    public static int getNumberOfImages(int processId) {
        try {
            return HistoryMysqlHelper.getNumberOfImages(processId);
        } catch (SQLException e) {
            log.error("Cannot get number of images", e);
        }
        return 0;
    }
}
