/*
 * Shared DataTables enhancements used across the manager UI.
 *
 *   var table = $('#myTable').DataTable($.extend(true, {},
 *       DTEnhance.standardOptions({ itemNamePlural: 'tilhørsforhold', excelTitle: 'Tilhørsforhold' }),
 *       { columnDefs: [ ... table-specific ... ] }
 *   ));
 *   DTEnhance.applyEnhancements(table);
 *
 * The template is expected to render a second <tr class="dt-column-filters"> row inside the
 * table's <thead>, with one <th><input type="text" class="form-control input-sm" placeholder="Filtrér"/></th>
 * per searchable column, and an empty <th></th> for any non-searchable column (e.g. an actions column).
 */
window.DTEnhance = (function () {

	function standardOptions(opts) {
		opts = opts || {};
		var itemNamePlural = opts.itemNamePlural || 'rækker';
		var pageLength = opts.pageLength || 50;
		var excelTitle = opts.excelTitle || 'Eksport';
		var excludeLastColFromExport = opts.excludeLastColFromExport !== false;

		return {
			pageLength: pageLength,
			dom: '<"html5buttons"B>lfrtip',
			buttons: [
				{
					extend: 'excel',
					title: excelTitle,
					exportOptions: excludeLastColFromExport ? { columns: ':not(:last-child)' } : {}
				}
			],
			bLengthChange: false,
			responsive: true,
			autoWidth: false,
			stateSave: true,
			stateDuration: -1,
			order: [[ 0, 'asc' ]],
			language: {
				search: 'Søg',
				lengthMenu: '_MENU_ ' + itemNamePlural + ' per side',
				info: 'Viser _START_ til _END_ af _TOTAL_ ' + itemNamePlural,
				zeroRecords: 'Ingen data...',
				infoEmpty: '',
				infoFiltered: '(ud af _MAX_ ' + itemNamePlural + ')',
				paginate: { previous: 'Forrige', next: 'Næste' }
			}
		};
	}

	function applyEnhancements(table) {
		var $tableEl = $(table.table().node());
		var $wrapper = $tableEl.closest('.dataTables_wrapper');
		var $globalInput = $wrapper.find('.dataTables_filter input');
		var tableId = $tableEl.attr('id') || ('dt-' + Math.random().toString(36).slice(2, 9));

		var $clearBtn = $('<button type="button" class="btn btn-sm btn-warning dt-clear-filters" style="display: none;">'
				+ '<em class="fa fa-times"></em> Ryd filtre '
				+ '<span class="dt-filter-count badge badge-light"></span>'
				+ '</button>')
				.attr('id', tableId + '-clearFilters');
		$clearBtn.prependTo($wrapper.find('.dataTables_filter'));

		function refreshIndicators() {
			var activeCount = 0;
			$tableEl.find('thead tr.dt-column-filters input').each(function () {
				if (this.value) {
					$(this).addClass('has-value');
					activeCount++;
				} else {
					$(this).removeClass('has-value');
				}
			});
			if (table.search()) {
				$globalInput.addClass('has-value');
				activeCount++;
			} else {
				$globalInput.removeClass('has-value');
			}
			if (activeCount > 0) {
				$clearBtn.show();
				$clearBtn.find('.dt-filter-count').text(activeCount);
			} else {
				$clearBtn.hide();
				$clearBtn.find('.dt-filter-count').text('');
			}
		}

		table.columns().every(function (colIdx) {
			var column = this;
			var input = $tableEl.find('thead tr.dt-column-filters th').eq(colIdx).find('input');
			if (!input.length) {
				return;
			}
			var saved = column.search();
			if (saved) {
				input.val(saved);
			}
			input.on('input keyup change clear', function () {
				if (column.search() !== this.value) {
					column.search(this.value).draw();
				}
				refreshIndicators();
			});
			input.on('click', function (e) { e.stopPropagation(); });
		});

		$globalInput.on('input keyup change', refreshIndicators);

		$clearBtn.on('click', function () {
			table.search('');
			table.columns().every(function () { this.search(''); });
			$tableEl.find('thead tr.dt-column-filters input').val('');
			$globalInput.val('');
			table.draw();
			refreshIndicators();
		});

		refreshIndicators();
	}

	return {
		standardOptions: standardOptions,
		applyEnhancements: applyEnhancements
	};
})();
