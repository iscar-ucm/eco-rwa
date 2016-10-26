function pareto_full_study
    numTests = 30;
    maxNumObjectives = 4;
    basePath = {'D:/jlrisco/Borrar/hero'};
    for no=2:maxNumObjectives
        for nt=1:numTests
            popAll = dlmread(strcat(basePath{1},'/population-all-',num2str(no),'-',num2str(nt),'.csv'),';'); 
            popRed = dlmread(strcat(basePath{1},'/population-reduced-',num2str(no),'-',num2str(nt),'.csv'),';'); 
            popAll = popAll(:,(size(popAll,2)-no+1):size(popAll,2));
            popRed = popRed(:,(size(popRed,2)-no+1):size(popRed,2));
            % Reduce population-all using the algorithm here:
            popRedMatLab = MOANonDominatedFront(popAll);
            popDiff = setdiff(popRed, popRedMatLab,'rows');
            if(size(popDiff)>0) 
                fprintf(strcat('There is a difference in -> HERO vs. MatLab [Objs=', num2str(no), ', Trial=',num2str(nt),']\n'));
            end
            if (no==2) 
                % Compare both of them:
                figure;
                plot(popRed(:,1), popRed(:,2), 'ob', popRedMatLab(:,1), popRedMatLab(:,2), '+r');
                xlabel('Obj 1');
                ylabel('Obj 2');
                legend('HERO','MatLab');
                title(sprintf(strcat('HERO vs. MatLab [Objs=', num2str(no), ', Trial=',num2str(nt),']')));
                grid on;
            end;
        end
    end
end

function F = MOANonDominatedFront(P)
	F = P;
	i = 1;
	while i>=1 && i<=(size(F,1)-1)
		j = i+1;
		while j>=(i+1) && j<=size(F,1)
			c = MoeaCompare(F(i,:)',F(j,:)');
			if c==0 
				F(j,:) = [];
				j = j-1;
			elseif c==-1 
				F(j,:) = [];
				j = j-1;
			elseif c==1 
				F(i,:) = [];
				i = i-1;
				j = size(F,1);
			end
			j = j+1;
		end
		i = i+1;
	end
end

function r = MoeaCompare(Pi, Pj)
	bigger = 0;
	smaller = 0; 
	indiff = 0;
	for i=1:size(Pi,1)
		if Pi(i)>Pj(i)  
			bigger = 1;
		elseif Pi(i)<Pj(i)  
			smaller = 1;
		end
		indiff = bigger*smaller;
		if indiff==1  
			break;
		end
	end
	if indiff==1 
		r = -2;
	elseif bigger==1 
		r = 1;
	elseif smaller==1 
		r = -1;
	else
		r = 0;
	end
end