/*
 * Copyright (C) 2014 Joseph Areeda <joseph.areeda at ligo.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ndsJUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A single availability record pointer for a single channel
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NdsChanAvailPair 
{
    private Integer idx;
    private Integer chanIdx;
    private Integer availIdx;
    
    public NdsChanAvailPair()
    {
        idx = 0;
        chanIdx = 0;
        availIdx = 0;
    }
    
    public NdsChanAvailPair(int idx, int chanIdx, int availIdx)
    {
        this.idx = idx;
        this.chanIdx = chanIdx;
        this.availIdx = availIdx;
    }
    
    public NdsChanAvailPair(ResultSet rs) throws SQLException
    {
        this.idx = rs.getInt("idx");
        chanIdx = rs.getInt("chanIdx");
        availIdx = rs.getInt("availIdx");
    }

    public Integer getIdx()
    {
        return idx;
    }

    public void setIdx(Integer idx)
    {
        this.idx = idx;
    }

    public Integer getChanIdx()
    {
        return chanIdx;
    }

    public void setChanIdx(Integer chanIdx)
    {
        this.chanIdx = chanIdx;
    }

    public Integer getAvailIdx()
    {
        return availIdx;
    }

    public void setAvailIdx(Integer availIdx)
    {
        this.availIdx = availIdx;
    }
    
}
