import { Component } from '@angular/core';
import { GridOptions, GridApi } from 'ag-grid-community';
import { default as file_data } from '../assets/data.json';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  title = 'angular-tabular-reporting';

  results = [];
  gridApi: any;
  gridColumnApi: any;
  statusBar: any;
  sideBar: any;
  tooltipShowDelay: any;

  myColumnDefs = [
    {
      headerName: 'Athlete',
      field: 'athlete',
      initialSort: 'asc',
    },
    {
      headerName: 'Age',
      field: 'age',
    },
    {
      headerName: 'Country',
      field: 'country',
    },
    {
      headerName: 'Year',
      field: 'year',
    },
    {
      headerName: 'Gold',
      field: 'gold',
    },
    {
      headerName: 'Silver',
      field: 'silver',
    },
    {
      headerName: 'Bronze',
      field: 'bronze',
    },
    {
      headerName: 'Total',
      field: 'total',
    },
  ]; // colDef end

  // default configs for all columns
  defaultColDef = {
    // #### sorting ####
    sortable: true,
    // unSortIcon: true,

    // #### filtering ####
    filter: 'agMultiColumnFilter',
    filterParams: {
      buttons: ['clear', 'apply'],
      showTooltips: true,
      applyMiniFilterWhileTyping: true,
    },
    floatingFilter: true,
    menuTabs: ['generalMenuTab', 'filterMenuTab', 'columnsMenuTab'],

    // ## rows / columns ####
    animateRows: true,
    resizable: true,
    minWidth: 90,

    // ### side-panel ####
    enableValue: true,
    enableRowGroup: true,
    enablePivot: true,

    // ## conditional highlighting ###
    cellStyle: (params) => {
      if (params.value === 1 || params.value === '1') {
        return { color: 'white', backgroundColor: '#660000' };
      }

      if (String(params.value).startsWith('United')) {
        return { color: 'white', backgroundColor: '#006600' };
      }

      if (String(params.vale).startsWith('FAIL')) {
        return { color: 'white', backgroundColor: '#660000' };
      }
      return null;
    },
  };

  constructor() {
    this.gridApi = GridApi;

    this.generate_json();

    this.tooltipShowDelay = 100;

    this.statusBar = {
      statusPanels: [
        { statusPanel: 'agTotalAndFilteredRowCountComponent', align: 'left' },
        { statusPanel: 'agTotalRowCountComponent', align: 'left' },
        {
          statusPanel: 'agAggregationComponent',
          align: 'left',
          statusPanelParams: {
            aggFuncs: ['count', 'sum', 'avg'],
          },
        },
      ],
    };

    this.sideBar = {
      hiddenByDefault: false,
      defaultToolPanel: 'columns',
      toolPanels: ['columns', 'filters'],
    };
  } // constructor end

  gridOptions = <GridOptions>{
    // below will directly read from  file hence expecting it as JSON,
    // rowData: file_data,

    // below is used when JSON contains only 1 array element in PSV (pipe-separated format)
    // conversion from TSV to JSON happens in generateJson() in constructor
    rowData: this.results,

    colWidth: 120,
    columnDefs: this.myColumnDefs,
    defaultColDef: this.defaultColDef,
  };

  onGridReady(params) {
    this.gridApi = params.api;
    this.gridColumnApi = params.gridColumnApi;
    params.api.sizeColumnsToFit();
  }

  onFirstDataRendered(params) {
    params.api.sizeColumnsToFit();
  }

  public generate_json() {
    // get all lines as array
    let lines = String(file_data).split('|');

    // get column headings
    let headers = lines[0].split(',');

    // iterate over each line excluding headers
    for (let i = 1; i < lines.length; i++) {
      let obj = {};
      let currLine = lines[i].split(',');

      // iterate to generate each column data for this line
      for (let j = 0; j < headers.length; j++) {
        obj[headers[j].trim()] = currLine[j].trim();
      } // iteration end over columns

      // add object to json array
      this.results.push(obj);
    } // iteration end over lines
  } // function-end
} // class-end
