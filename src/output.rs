/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                           *
 *  Copyright  (C)  2015-2024  Christian Krause                              *
 *                                                                           *
 *  Christian Krause  <christian.krause@mailbox.org>                         *
 *                                                                           *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                           *
 *  This file is part of strace-analyzer.                                    *
 *                                                                           *
 *  strace-analyzer is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by     *
 *  the Free Software Foundation, either version 3 of the license, or any    *
 *  later version.                                                           *
 *                                                                           *
 *  strace-analyzer is distributed in the hope that it will be useful, but   *
 *  WITHOUT ANY WARRANTY; without even the implied warranty of               *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU         *
 *  General Public License for more details.                                 *
 *                                                                           *
 *  You should have received a copy of the GNU General Public License along  *
 *  with strace-analyzer. If not, see <http://www.gnu.org/licenses/>.        *
 *                                                                           *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

use std::str::FromStr;

use anyhow::{anyhow, Result};
use clap::builder::PossibleValue;
use clap::ValueEnum;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Output {
    Continuous,
    #[cfg(feature = "table")]
    Table,
}

impl Output {
    pub const fn name(self) -> &'static str {
        match self {
            Self::Continuous => "continuous",
            #[cfg(feature = "table")]
            Self::Table => "table",
        }
    }
}

impl Default for Output {
    fn default() -> Self {
        if cfg!(feature = "table") {
            Self::Table
        } else {
            Self::Continuous
        }
    }
}

impl FromStr for Output {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let s = s.to_lowercase();
        let s = s.as_str();

        match s {
            "continuous" => Ok(Self::Continuous),
            #[cfg(feature = "table")]
            "table" => Ok(Self::Table),
            _ => Err(anyhow!("invalid output")),
        }
    }
}

impl ValueEnum for Output {
    fn value_variants<'a>() -> &'a [Self] {
        &[
            Self::Continuous,
            #[cfg(feature = "table")]
            Self::Table,
        ]
    }

    fn to_possible_value(&self) -> Option<PossibleValue> {
        Some(PossibleValue::new(self.name()))
    }
}
